package org.etd.framework.starter.ai.runtime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.framework.starter.ai.runtime.model.AgentExecutionRequest;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEvent;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEventType;
import org.etd.framework.starter.ai.runtime.model.AgentRunRequest;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeResult;
import org.etd.framework.starter.ai.runtime.model.AgentRunProgress;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;
import org.etd.framework.starter.ai.runtime.spi.AgentRunRecordService;
import java.util.function.Supplier;
import org.etd.framework.starter.ai.runtime.model.KnowledgeFragment;
import org.etd.framework.starter.ai.runtime.spi.AgentRunEventListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 不依赖HTTP或业务数据库，验证框架通过插槽完成输出、持久化、上下文及取消。 */
class AgentRuntimeTest {
    /** 清理测试线程上下文。 */
    @AfterEach
    void clear() {
        RequestContext.clean();
    }

    /** 外部业务只实现插槽即可使用完整运行生命周期，框架输出不绑定SSE。 */
    @Test
    void executesThroughLifecycleAndOutputSlots() throws Exception {
        RequestContext.setTenantCode(1L);
        ChatModel model = mock(ChatModel.class);
        AgentRunRecordService recordService = recordService();
        CapturingOutput output = new CapturingOutput();
        ChatResponse first = response("你");
        ChatResponse second = response("好");
        when(model.stream(any(Prompt.class))).thenAnswer(call -> {
            assertThat(RequestContext.getTenantCode()).isEqualTo(1L);
            return Flux.just(first, second);
        });
        AgentRunRequest runRequest = runRequest(model, recordService, output);
        AgentExecutionEngine engine = new AgentExecutionEngine();
        AgentRuntimeProperties properties = new AgentRuntimeProperties();
        List<AgentRunRecordService> providers = List.of(recordService);
        try (AgentRuntime runtime = new AgentRuntime(engine, properties, () -> providers)) {
            runtime.start(() -> runRequest);
            assertThat(output.closed.await(5, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(output.events).extracting(AgentRuntimeEvent::type).containsExactly(
                AgentRuntimeEventType.START, AgentRuntimeEventType.REFERENCES, AgentRuntimeEventType.DELTA,
                AgentRuntimeEventType.DELTA, AgentRuntimeEventType.TERMINAL);
        ArgumentCaptor<AgentRuntimeResult> captured = ArgumentCaptor.forClass(AgentRuntimeResult.class);
        verify(recordService).finishRun(captured.capture());
        assertThat(captured.getValue().progress().content()).isEqualTo("你好");
        assertThat(captured.getValue().status()).isEqualTo(AgentRuntimeStatus.SUCCEEDED);
    }

    /** 生命周期插槽返回取消状态时，框架终止模型订阅并交回部分正文。 */
    @Test
    void cancelsFromBusinessLifecycleSignal() throws Exception {
        ChatModel model = mock(ChatModel.class);
        AgentRunRecordService recordService = recordService();
        when(recordService.renewRunLease("run-one")).thenReturn(AgentRuntimeStatus.CANCELLED);
        AtomicBoolean cancelled = new AtomicBoolean();
        ChatResponse first = response("部分");
        when(model.stream(any(Prompt.class))).thenReturn(Flux.concat(Flux.just(first), Flux.never())
                .doOnCancel(() -> cancelled.set(true)));
        CapturingOutput output = new CapturingOutput();
        AgentRunRequest runRequest = runRequest(model, recordService, output);
        AgentExecutionEngine engine = new AgentExecutionEngine();
        AgentRuntimeProperties properties = new AgentRuntimeProperties();
        List<AgentRunRecordService> providers = List.of(recordService);
        try (AgentRuntime runtime = new AgentRuntime(engine, properties, () -> providers)) {
            runtime.start(() -> runRequest);
            assertThat(output.closed.await(5, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(cancelled).isTrue();
        AgentRuntimeResult terminal = output.events.getLast().result();
        assertThat(terminal.status()).isEqualTo(AgentRuntimeStatus.CANCELLED);
        assertThat(terminal.progress().content()).isEqualTo("部分");
    }

    /** 容量拒绝不执行业务准备插槽；检查点回调保存部分正文，主动取消后释放运行位置。 */
    @Test
    void boundsCapacityBeforePreparationAndInvokesCheckpointSlot() throws Exception {
        ChatModel model = mock(ChatModel.class);
        ChatResponse first = response("检查点正文");
        when(model.stream(any(Prompt.class))).thenReturn(Flux.concat(Flux.just(first), Flux.never()));
        AgentRunRecordService recordService = recordService();
        CountDownLatch checkpoint = new CountDownLatch(1);
        doAnswer(call -> {
            checkpoint.countDown();
            return null;
        }).when(recordService).saveRunProgress(any());
        CapturingOutput output = new CapturingOutput();
        AgentRunRequest runRequest = runRequest(model, recordService, output);
        AgentExecutionEngine engine = new AgentExecutionEngine();
        AgentRuntimeProperties properties = new AgentRuntimeProperties();
        properties.setMaxConcurrentRuns(1);
        properties.setCheckpointMillis(500);
        List<AgentRunRecordService> providers = List.of(recordService);
        AtomicBoolean rejectedPreparation = new AtomicBoolean();
        try (AgentRuntime runtime = new AgentRuntime(engine, properties, () -> providers)) {
            AgentRunControl handle = runtime.start(() -> runRequest);
            assertThatThrownBy(() -> runtime.start(() -> {
                rejectedPreparation.set(true);
                return runRequest;
            })).hasMessageContaining("任务较多");
            assertThat(rejectedPreparation).isFalse();
            assertThat(checkpoint.await(5, TimeUnit.SECONDS)).isTrue();
            handle.cancel();
            assertThat(output.closed.await(5, TimeUnit.SECONDS)).isTrue();
        }
        ArgumentCaptor<AgentRunProgress> captured = ArgumentCaptor.forClass(AgentRunProgress.class);
        verify(recordService).saveRunProgress(captured.capture());
        assertThat(captured.getValue().content()).isEqualTo("检查点正文");
    }

    private AgentRunRecordService recordService() {
        AgentRunRecordService recordService = mock(AgentRunRecordService.class);
        when(recordService.renewRunLease("run-one")).thenReturn(AgentRuntimeStatus.RUNNING);
        when(recordService.finishRun(any())).thenAnswer(call -> call.getArgument(0));
        return recordService;
    }

    /** 官方图会过滤无正文的统计事件，框架仍须保存最终用量，且不能重复追加图的聚合正文。 */
    @Test
    void recordsUsageOnlyFinalChunkWithoutDuplicatingAnswer() throws Exception {
        ChatModel model = mock(ChatModel.class);
        ChatResponse answer = response("完整回答");
        DefaultUsage usage = new DefaultUsage(120, 8);
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        List<Generation> empty = List.of();
        ChatResponse statistics = new ChatResponse(empty, metadata);
        Flux<ChatResponse> stream = Flux.just(answer, statistics);
        when(model.stream(any(Prompt.class))).thenReturn(stream);
        AgentRunRecordService records = recordService();
        CapturingOutput output = new CapturingOutput();
        AgentRunRequest request = runRequest(model, records, output);
        runToCompletion(request, records, output);
        AgentRuntimeResult result = output.events.getLast().result();
        assertThat(result.status()).isEqualTo(AgentRuntimeStatus.SUCCEEDED);
        assertThat(result.progress().content()).isEqualTo("完整回答");
        assertThat(result.progress().inputTokens()).isEqualTo(120L);
        assertThat(result.progress().outputTokens()).isEqualTo(8L);
    }

    /** SDK 创建流时直接抛错也必须进入失败终态，不能将上游异常文本持久化或推给前端。 */
    @Test
    void failsWhenModelThrowsBeforeReturningStream() throws Exception {
        ChatModel model = mock(ChatModel.class);
        IllegalStateException failure = new IllegalStateException("private-model-detail");
        when(model.stream(any(Prompt.class))).thenThrow(failure);
        AgentRunRecordService records = recordService();
        CapturingOutput output = new CapturingOutput();
        AgentRunRequest request = runRequest(model, records, output);
        runToCompletion(request, records, output);
        AgentRuntimeResult result = output.events.getLast().result();
        assertThat(result.status()).isEqualTo(AgentRuntimeStatus.FAILED);
        assertThat(result.progress().content()).isEmpty();
        assertThat(result.errorMessage()).doesNotContain("private-model-detail");
    }

    /** 使用真实官方图执行至关闭，统一释放本测试拥有的线程容器。 */
    private void runToCompletion(AgentRunRequest request, AgentRunRecordService records,
            CapturingOutput output) throws Exception {
        AgentExecutionEngine engine = new AgentExecutionEngine();
        AgentRuntimeProperties properties = new AgentRuntimeProperties();
        List<AgentRunRecordService> providers = List.of(records);
        try (AgentRuntime runtime = new AgentRuntime(engine, properties, () -> providers)) {
            runtime.start(() -> request);
            assertThat(output.closed.await(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private AgentRunRequest runRequest(ChatModel model, AgentRunRecordService recordService, CapturingOutput output) {
        Supplier<List<List<KnowledgeFragment>>> knowledgeService = () -> List.of();
        ChatOptions options = ChatOptions.builder().maxTokens(100).build();
        AgentExecutionRequest request = new AgentExecutionRequest("帮助用户", "问题", List.of(), options, knowledgeService, 1000, 12, true);
        Map<String, Object> attributes = Map.<String, Object>of("caller", "test");
        return new AgentRunRequest("run-one", request, () -> model, recordService, output, attributes);
    }

    private ChatResponse response(String text) {
        AssistantMessage message = new AssistantMessage(text);
        Generation generation = new Generation(message);
        List<Generation> generations = List.of(generation);
        return new ChatResponse(generations);
    }

    /** 测试输出接收端，证明框架不需要Servlet或SseEmitter。 */
    private static final class CapturingOutput implements AgentRunEventListener {
        private final List<AgentRuntimeEvent> events = new CopyOnWriteArrayList<>();
        private final CountDownLatch closed = new CountDownLatch(1);
        @Override
        public void onEvent(AgentRuntimeEvent event) {
            events.add(event);
        }
        @Override
        public void close() {
            closed.countDown();
        }
    }
}
