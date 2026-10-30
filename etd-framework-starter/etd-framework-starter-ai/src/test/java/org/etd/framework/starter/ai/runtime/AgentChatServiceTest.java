package org.etd.framework.starter.ai.runtime;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.framework.starter.ai.runtime.model.AgentChatPreparation;
import org.etd.framework.starter.ai.runtime.model.AgentChatRequest;
import org.etd.framework.starter.ai.runtime.model.AgentKnowledgeQuery;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEvent;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeResult;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;
import org.etd.framework.starter.ai.runtime.spi.AgentChatPrepareService;
import org.etd.framework.starter.ai.runtime.spi.AgentKnowledgeQueryService;
import org.etd.framework.starter.ai.runtime.spi.AgentRunEventListener;
import org.etd.framework.starter.ai.runtime.spi.AgentRunRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 通过统一入口验证接口协作、追问重写、准备失败补偿与容量限制，业务不构造内部执行对象。 */
class AgentChatServiceTest {
    /** 内部线程容器，测试结束必须关闭。 */
    private AgentRuntime runtime;
    /** 对外统一入口。 */
    private AgentChatService chat;
    /** 只返回本轮业务数据的准备接口。 */
    private AgentChatPrepareService preparation;
    /** 只接收最终检索参数的业务接口。 */
    private AgentKnowledgeQueryService knowledge;
    /** 记录接口用于核对最终状态及补偿。 */
    private AgentRunRecordService records;
    /** 模型入口替身，验证框架负责解析模型。 */
    private ModelClientRegistry models;
    /** 不连接任何收费服务。 */
    private ChatModel model;
    /** 本轮业务数据，可调整以验证不同接入条件。 */
    private AgentChatPreparation data;
    /** 使用最小容量验证准备回调之前的拒绝行为。 */
    private AgentRuntimeProperties properties;

    /** 业务只实现三个接口，执行组装和模型选项全部使用框架生产逻辑。 */
    @BeforeEach
    void initialize() {
        preparation = mock(AgentChatPrepareService.class);
        knowledge = mock(AgentKnowledgeQueryService.class);
        records = mock(AgentRunRecordService.class);
        models = mock(ModelClientRegistry.class);
        model = mock(ChatModel.class);
        properties = new AgentRuntimeProperties();
        properties.setMaxConcurrentRuns(1);
        AgentExecutionEngine engine = new AgentExecutionEngine();
        runtime = new AgentRuntime(engine, properties, () -> List.of(records));
        chat = new AgentChatService(runtime, properties, models, preparation, knowledge, records);
        data = preparationData();
        when(preparation.prepareChat(any())).thenReturn(data);
        when(models.getChatModel("1", "100")).thenReturn(model);
        when(knowledge.queryKnowledgeList(any())).thenReturn(List.of());
        when(records.renewRunLease(anyString())).thenReturn(AgentRuntimeStatus.RUNNING);
        when(records.finishRun(any())).thenAnswer(call -> call.getArgument(0, AgentRuntimeResult.class));
        ChatResponse answer = response("最终回答");
        Flux<ChatResponse> stream = Flux.just(answer);
        when(model.stream(any(Prompt.class))).thenReturn(stream);
    }

    /** 关闭线程及定时任务，不能将运行留给下一测试。 */
    @AfterEach
    void close() {
        runtime.close();
    }

    /** 框架使用最近四条历史重写检索词，最终回答仍使用原问题，且自动绑定输出控制器。 */
    @Test
    void assemblesExecutionAndRewritesQueryInsideFramework() throws Exception {
        ChatResponse rewritten = response("  打印机Wi-Fi配置步骤  ");
        when(model.call(any(Prompt.class))).thenReturn(rewritten);
        RecordingOutput output = runChat();
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(model).call(prompt.capture());
        // 官方压缩器将最近历史格式化到一条用户消息，系统规则保持独立。
        assertThat(prompt.getValue().getInstructions()).hasSize(2);
        assertThat(prompt.getValue().getContents()).contains("最近问题", "最新追问：它怎么配置").doesNotContain("最早问题");
        assertThat(prompt.getValue().getOptions().getMaxTokens()).isEqualTo(64);
        assertThat(prompt.getValue().getOptions().getTemperature()).isEqualTo(0.1);
        verifyQuery("打印机Wi-Fi配置步骤");
        ArgumentCaptor<Prompt> answer = ArgumentCaptor.forClass(Prompt.class);
        verify(model).stream(answer.capture());
        assertThat(answer.getValue().getContents()).contains("它怎么配置");
        assertThat(answer.getValue().getOptions().getMaxTokens()).isEqualTo(properties.getDefaultMaxOutputTokens());
        assertThat(output.control).isNotNull();
        verify(preparation).prepareChat(any());
    }

    /** 无历史直接检索，不产生额外重写请求。 */
    @Test
    void skipsRewriteWithoutHistory() throws Exception {
        data.setHistory(List.of());
        runChat();
        verify(model, never()).call(any(Prompt.class));
        verifyQuery("它怎么配置");
    }

    /** 没有绑定知识库时，不调用检索接口，也不进行追问重写。 */
    @Test
    void skipsKnowledgeAndRewriteWhenNoBaseIsBound() throws Exception {
        data.setKnowledgeBaseIds(List.of());
        runChat();
        verifyNoInteractions(knowledge);
        verify(model, never()).call(any(Prompt.class));
    }

    /** 模型未返回有效重写内容时，保留既有的原问题检索策略。 */
    @Test
    void retainsOriginalQueryWhenRewriteHasNoResponse() throws Exception {
        runChat();
        verifyQuery("它怎么配置");
    }

    /** 重写失败仍按原问题检索；最终回答模型请求继续由框架执行。 */
    @Test
    void retainsOriginalQueryWhenRewriteFails() throws Exception {
        IllegalStateException failure = new IllegalStateException("synthetic-model-failure");
        when(model.call(any(Prompt.class))).thenThrow(failure);
        runChat();
        verifyQuery("它怎么配置");
    }

    /** 准备事务已提交但返回数据无效时，也必须补记失败，不能留下长期运行的记录。 */
    @Test
    void finishesRunWhenPreparationDataIsInvalid() {
        data.setModelConnectionId(null);
        RecordingOutput output = new RecordingOutput();
        AgentChatRequest request = request();
        assertThatThrownBy(() -> chat.start(request, output)).hasMessageContaining("模型连接");
        ArgumentCaptor<AgentRuntimeResult> result = ArgumentCaptor.forClass(AgentRuntimeResult.class);
        verify(records).finishRun(result.capture());
        assertThat(result.getValue().status()).isEqualTo(AgentRuntimeStatus.FAILED);
        assertThat(output.closed.getCount()).isZero();
        verifyNoInteractions(models, knowledge);
    }

    /** 统一入口不能绕过容量限制，拒绝请求不得再调用业务准备接口创建记录。 */
    @Test
    void rejectsCapacityBeforeCallingBusinessPreparation() {
        Flux<ChatResponse> stream = Flux.never();
        when(model.stream(any(Prompt.class))).thenReturn(stream);
        RecordingOutput output = new RecordingOutput();
        AgentChatRequest request = request();
        AgentRunControl control = chat.start(request, output);
        try {
            assertThatThrownBy(() -> chat.start(request, output)).hasMessageContaining("任务较多");
            verify(preparation, times(1)).prepareChat(any());
        } finally {
            control.cancel();
        }
    }

    /** 输出关闭代表记录和终态处理已完成，避免根据sleep推测异步执行结果。 */
    private RecordingOutput runChat() throws Exception {
        RecordingOutput output = new RecordingOutput();
        AgentChatRequest request = request();
        chat.start(request, output);
        assertThat(output.closed.await(5, TimeUnit.SECONDS)).isTrue();
        return output;
    }

    /** 核对业务只收到已确定的检索词和知识库。 */
    private void verifyQuery(String expected) {
        ArgumentCaptor<AgentKnowledgeQuery> captured = ArgumentCaptor.forClass(AgentKnowledgeQuery.class);
        verify(knowledge).queryKnowledgeList(captured.capture());
        assertThat(captured.getValue().question()).isEqualTo(expected);
        assertThat(captured.getValue().knowledgeBaseIds()).containsExactly(10L);
    }

    /** 固定当前问题以确认重写只影响检索，不覆盖回答输入。 */
    private AgentChatRequest request() {
        return new AgentChatRequest(1L, "chat-test", "它怎么配置");
    }

    /** 模拟业务返回的数据，不携带模型实例或任何可执行回调。 */
    private AgentChatPreparation preparationData() {
        AgentChatPreparation result = new AgentChatPreparation();
        result.setExecutionId("3");
        result.setModelTenantId("1");
        result.setModelConnectionId("100");
        result.setSystemPrompt("回答问题");
        List<Long> ids = List.of(10L);
        result.setKnowledgeBaseIds(ids);
        UserMessage oldQuestion = new UserMessage("最早问题");
        AssistantMessage oldAnswer = new AssistantMessage("最早答案");
        UserMessage question = new UserMessage("打印机支持什么连接方式");
        AssistantMessage answer = new AssistantMessage("支持Wi-Fi连接");
        UserMessage recentQuestion = new UserMessage("最近问题");
        AssistantMessage recentAnswer = new AssistantMessage("最近答案");
        List<Message> history = List.of(oldQuestion, oldAnswer, question, answer, recentQuestion, recentAnswer);
        result.setHistory(history);
        return result;
    }

    /** 创建本地模型响应。 */
    private ChatResponse response(String text) {
        AssistantMessage message = new AssistantMessage(text);
        Generation generation = new Generation(message);
        List<Generation> generations = List.of(generation);
        return new ChatResponse(generations);
    }

    /** 输出适配器只处理协议接收和取消入口绑定，不参与执行组装。 */
    private static final class RecordingOutput implements AgentRunEventListener {
        /** 运行完成信号。 */
        private final CountDownLatch closed = new CountDownLatch(1);
        /** 框架自动绑定的本轮取消入口。 */
        private AgentRunControl control;

        @Override
        public void bind(AgentRunControl control) {
            this.control = control;
        }

        @Override
        public void onEvent(AgentRuntimeEvent event) {
        }

        @Override
        public void close() {
            closed.countDown();
        }
    }
}
