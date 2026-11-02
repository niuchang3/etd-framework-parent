package org.etd.framework.starter.ai.agent;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.etd.framework.starter.ai.memory.AgentMemoryManager;
import org.etd.framework.starter.ai.runtime.AgentRunControl;
import org.etd.framework.starter.ai.agent.AgentExecutionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.ai.model.client.ModelRequestHeaders;
import org.etd.framework.starter.ai.runtime.model.AgentChatPreparation;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.ContextView;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.etd.framework.starter.ai.memory.semantic.AgentKnowledgeQueryService;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.framework.starter.ai.tooling.AgentToolAuthorization;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 订阅真实官方图，验证工具观察与历史进入后续模型调用，禁止用只构建 Flux 代替验收。 */
class AgentExecutorTest {
    /** 工具参数没有租户与用户字段，可信身份从执行上下文读取。 */
    record LookupInput(String query) { }

    @AfterEach
    void cleanContext() {
        RequestContext.clean();
    }

    @Test
    void lazySubscriptionRestoresCapturedIdentityBeforeModelResolution() {
        RequestContext.setTenantCode(9L);
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> streamWithCapturedIdentity());
        ModelClientRegistry models = mock(ModelClientRegistry.class);
        when(models.getChatModel(any(), any())).thenAnswer(invocation -> {
            assertThat(RequestContext.getTenantCode()).isEqualTo(9L);
            return model;
        });
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("models", models);
        AgentMemoryManager memory = new AgentMemoryManager();
        AgentRuntimeProperties properties = new AgentRuntimeProperties();
        ReactAgentExecutor executor = new ReactAgentExecutor(memory, null, beans.getBeanProvider(ModelClientRegistry.class),
                beans.getBeanProvider(AgentKnowledgeQueryService.class), beans.getBeanProvider(ToolCallbackProvider.class), properties);
        AgentChatPreparation prepared = new AgentChatPreparation("scope", "9", "100", "系统指令", List.of(),
                null, null, List.of(), List.of(), false, null, null, null, null, Map.of());
        AgentRunControl control = mock(AgentRunControl.class);
        Flux<String> stream = executor.execute(prepared, "问题", control);
        RequestContext.clean();
        List<String> result = stream.subscribeOn(Schedulers.boundedElastic()).collectList().block(Duration.ofSeconds(5));
        assertThat(result).containsExactly("作用域回答");
        assertThat(RequestContext.getTenantCode()).isNull();
    }

    private Flux<ChatResponse> streamWithCapturedIdentity() {
        assertThat(RequestContext.getTenantCode()).isEqualTo(9L);
        return Flux.deferContextual(this::responseWithCapturedHeaders);
    }

    private Flux<ChatResponse> responseWithCapturedHeaders(ContextView context) {
        Map<String, String> headers = context.get(ModelRequestHeaders.REACTOR_KEY);
        assertThat(headers).containsEntry("tenant-code", "9");
        ChatResponse response = answer("作用域回答");
        return Flux.just(response);
    }

    @Test
    void retainsHistoryAndSystemRoleWithoutTools() {
        AtomicReference<Prompt> captured = new AtomicReference<>();
        ChatModel model = mock(ChatModel.class);
        ChatResponse answer = answer("回答");
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return Flux.just(answer);
        });
        AgentMemoryManager memory = new AgentMemoryManager();
        ReactAgentExecutor executor = createExecutor(memory, null);
        UserMessage oldQuestion = new UserMessage("旧问题");
        AssistantMessage oldAnswer = new AssistantMessage("旧答案");
        SystemMessage historyRule = new SystemMessage("已授权的历史系统信息");
        List<Message> history = List.of(oldQuestion, historyRule, oldAnswer);
        ChatOptions options = ChatOptions.builder().temperature(0.3).build();
        AgentExecutionRequest request = new AgentExecutionRequest("系统规则", "新问题", history,
                options, List.of(), 2000, 3, false);
        AgentRunControl control = mock(AgentRunControl.class);
        List<String> output = executor.execute(model, request, control).collectList().block(Duration.ofSeconds(5));
        assertThat(output).containsExactly("回答");
        assertThat(captured.get().getInstructions().getFirst()).isInstanceOf(SystemMessage.class);
        assertThat(captured.get().getInstructions()).contains(oldQuestion, historyRule, oldAnswer);
        assertThat(captured.get().getOptions().getTemperature()).isEqualTo(0.3);
        verify(control).recordReferences(List.of());
    }

    @Test
    void officialReactLoopReturnsToolObservationToNextModel() {
        AtomicInteger modelCalls = new AtomicInteger();
        AtomicInteger toolCalls = new AtomicInteger();
        AtomicInteger authorizations = new AtomicInteger();
        ChatModel model = mock(ChatModel.class);
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("lookup-1", "function", "lookup", "{\"query\":\"资料\"}");
        List<AssistantMessage.ToolCall> calls = List.of(toolCall);
        AssistantMessage decision = AssistantMessage.builder().content("").toolCalls(calls).build();
        Generation generation = new Generation(decision);
        ChatResponse first = new ChatResponse(List.of(generation));
        ChatResponse second = answer("已取得工具结果");
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            if (modelCalls.incrementAndGet() == 1) {
                return Flux.just(first);
            }
            assertThat(prompt.getInstructions()).anySatisfy(message -> {
                assertThat(message).isInstanceOf(ToolResponseMessage.class);
                ToolResponseMessage observation = (ToolResponseMessage) message;
                assertThat(observation.getResponses().getFirst().responseData()).contains("真实观察");
            });
            return Flux.just(second);
        });
        ToolCallback tool = FunctionToolCallback.builder("lookup", (LookupInput input) -> {
            toolCalls.incrementAndGet();
            return "真实观察";
        }).description("授权资料查询").inputType(LookupInput.class).build();
        ToolCallingChatOptions options = ToolCallingChatOptions.builder().toolCallbacks(tool).build();
        AgentExecutionRequest request = new AgentExecutionRequest("系统规则", "问题", List.of(),
                options, List.of(), 4000, 3, false);
        AgentMemoryManager memory = new AgentMemoryManager();
        ReactAgentExecutor executor = createExecutor(memory, name -> authorizations.incrementAndGet());
        AgentRunControl control = mock(AgentRunControl.class);
        List<String> result = executor.execute(model, request, control).collectList().block(Duration.ofSeconds(5));
        assertThat(result).containsExactly("已取得工具结果");
        assertThat(modelCalls.get()).isEqualTo(2);
        assertThat(toolCalls.get()).isEqualTo(1);
        assertThat(authorizations.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void refusesInsufficientTokenBudgetBeforeProviderCall() {
        ChatModel model = mock(ChatModel.class);
        AgentMemoryManager memory = new AgentMemoryManager();
        ReactAgentExecutor executor = createExecutor(memory, null);
        AgentExecutionRequest request = new AgentExecutionRequest("系统规则", "问题", List.of(),
                null, List.of(), 1, 3, false);
        AgentRunControl control = mock(AgentRunControl.class);
        assertThatThrownBy(() -> executor.execute(model, request, control).blockLast(Duration.ofSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void repeatedQuestionStillAllowsOldTurnTrimmingAfterToolObservation() {
        AtomicInteger calls = new AtomicInteger();
        ChatModel model = mock(ChatModel.class);
        UserMessage oldQuestion = new UserMessage("问题");
        AssistantMessage oldAnswer = new AssistantMessage("history ".repeat(1500));
        List<Message> history = List.of(oldQuestion, oldAnswer);
        AssistantMessage.ToolCall call = new AssistantMessage.ToolCall("repeat-1", "function", "lookup", "{\"query\":\"资料\"}");
        List<AssistantMessage.ToolCall> toolCalls = List.of(call);
        AssistantMessage decision = AssistantMessage.builder().content("").toolCalls(toolCalls).build();
        Generation generation = new Generation(decision);
        ChatResponse first = new ChatResponse(List.of(generation));
        ChatResponse finalAnswer = answer("回答");
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> {
            if (calls.incrementAndGet() == 1) {
                return Flux.just(first);
            }
            Prompt prompt = invocation.getArgument(0);
            assertThat(prompt.getInstructions()).doesNotContain(oldAnswer);
            return Flux.just(finalAnswer);
        });
        ToolCallback callback = FunctionToolCallback.builder("lookup", (LookupInput input) -> "observation ".repeat(1800))
                .description("重复问题窗口测试").inputType(LookupInput.class).build();
        ToolCallingChatOptions options = ToolCallingChatOptions.builder().toolCallbacks(callback).build();
        AgentExecutionRequest request = new AgentExecutionRequest("系统规则", "问题", history, options, List.of(), 3000, 3, false);
        AgentMemoryManager memory = new AgentMemoryManager();
        AgentToolAuthorization authorization = name -> { };
        ReactAgentExecutor executor = createExecutor(memory, authorization);
        AgentRunControl control = mock(AgentRunControl.class);
        assertThat(executor.execute(model, request, control).collectList().block(Duration.ofSeconds(5))).containsExactly("回答");
        assertThat(calls.get()).isEqualTo(2);
    }

    private ReactAgentExecutor createExecutor(AgentMemoryManager memory, AgentToolAuthorization authorization) {
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        AgentRuntimeProperties properties = new AgentRuntimeProperties();
        return new ReactAgentExecutor(memory, authorization, beans.getBeanProvider(ModelClientRegistry.class),
                beans.getBeanProvider(AgentKnowledgeQueryService.class), beans.getBeanProvider(ToolCallbackProvider.class),
                properties);
    }

    private ChatResponse answer(String text) {
        AssistantMessage message = new AssistantMessage(text);
        Generation generation = new Generation(message);
        List<Generation> generations = List.of(generation);
        return new ChatResponse(generations);
    }
}
