package org.etd.framework.starter.ai.runtime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.etd.framework.starter.ai.adapter.AgentRunEventListener;
import org.etd.framework.starter.ai.agent.AgentExecutor;
import org.etd.framework.starter.ai.agent.ReactAgentExecutor;
import org.etd.framework.starter.ai.memory.AgentMemoryManager;
import org.etd.framework.starter.ai.persistence.spi.AgentRunStore;
import org.etd.framework.starter.ai.persistence.spi.AgentRunLeaseStore;
import org.etd.framework.starter.ai.runtime.model.AgentChatCommand;
import org.etd.framework.starter.ai.runtime.model.AgentChatPreparation;
import org.etd.framework.starter.ai.runtime.spi.AgentChatPrepareService;
import org.etd.framework.starter.ai.memory.semantic.AgentKnowledgeQueryService;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEvent;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEventType;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.core.Disposable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AgentChatService 统一门面服务测试")
class AgentChatServiceTest {

    private AgentTaskRunner taskRunner;
    private AgentChatService chatService;
    private AgentRunStore runStore;
    private ChatModel model;
    private AgentRuntimeProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AgentRuntimeProperties();
        properties.setMaxConcurrentRuns(1);

        runStore = mock(AgentRunStore.class);
        AgentRunLeaseStore leases = mock(AgentRunLeaseStore.class);
        when(leases.renewRunLease(any())).thenReturn(AgentRuntimeStatus.RUNNING);
        when(runStore.finishRun(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AgentMemoryManager memoryManager = new AgentMemoryManager();
        taskRunner = new AgentTaskRunner(properties, leaseProvider(leases));

        model = mock(ChatModel.class);
        ChatResponse response = new ChatResponse(List.of(new Generation(new AssistantMessage("这是回答"))));
        when(model.stream(any(Prompt.class))).thenReturn(Flux.just(response));
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        AgentChatPrepareService preparation = command -> new AgentChatPreparation(command.requestId(), "tenant", "model",
                "系统指令", List.of(), null, null, List.of(), List.of(), false, null, null, null, null, Map.of());
        ModelClientRegistry models = mock(ModelClientRegistry.class);
        when(models.getChatModel(any(), any())).thenReturn(model);
        beans.registerSingleton("preparation", preparation);
        beans.registerSingleton("records", runStore);
        beans.registerSingleton("leases", leases);
        beans.registerSingleton("models", models);
        AgentExecutor executor = new ReactAgentExecutor(memoryManager, null, beans.getBeanProvider(ModelClientRegistry.class),
                beans.getBeanProvider(AgentKnowledgeQueryService.class), beans.getBeanProvider(ToolCallbackProvider.class), properties);
        chatService = new AgentChatService(taskRunner, executor, properties,
                beans.getBeanProvider(AgentChatPrepareService.class), beans.getBeanProvider(AgentRunStore.class),
                beans.getBeanProvider(AgentRunLeaseStore.class));

    }

    @AfterEach
    void tearDown() {
        taskRunner.close();
    }

    @Test
    @DisplayName("通过门面服务启动对话任务并流式接收结果")
    void shouldStartChatTaskSuccessfully() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> contentRef = new AtomicReference<>("");
        AtomicReference<String> runRef = new AtomicReference<>();

        AgentRunEventListener listener = new AgentRunEventListener() {
            @Override
            public void onEvent(AgentRuntimeEvent event) {
                if (event.type() == AgentRuntimeEventType.START) {
                    runRef.set(event.progress().runId());
                } else if (event.type() == AgentRuntimeEventType.DELTA) {
                    contentRef.updateAndGet(s -> s + event.text());
                } else if (event.type() == AgentRuntimeEventType.TERMINAL) {
                    latch.countDown();
                }
            }
        };

        AgentChatCommand command = new AgentChatCommand(1L, "exec-101", "问题");
        Disposable control = chatService.start(command, listener);
        assertThat(control).isNotNull();

        boolean finished = latch.await(3, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        assertThat(contentRef.get()).isEqualTo("这是回答");
        assertThat(runRef.get()).isEqualTo("exec-101");

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(model).stream(promptCaptor.capture());
        assertThat(promptCaptor.getValue().getInstructions()).isNotEmpty();
    }

    @Test
    @DisplayName("容量占满时拒绝提交新任务")
    void shouldRejectWhenCapacityIsFull() {
        when(model.stream(any(Prompt.class))).thenReturn(Flux.never());

        AgentChatCommand first = new AgentChatCommand(1L, "exec-1", "问题");
        AgentChatCommand second = new AgentChatCommand(2L, "exec-2", "问题");
        AgentRunEventListener output = event -> { };
        Disposable control = chatService.start(first, output);
        try {
            assertThatThrownBy(() -> chatService.start(second, output))
                    .hasMessageContaining("当前生成任务较多");
        } finally {
            control.dispose();
        }
    }
    private static ObjectProvider<AgentRunLeaseStore> leaseProvider(AgentRunLeaseStore lease) {
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("lease", lease);
        return beans.getBeanProvider(AgentRunLeaseStore.class);
    }

}
