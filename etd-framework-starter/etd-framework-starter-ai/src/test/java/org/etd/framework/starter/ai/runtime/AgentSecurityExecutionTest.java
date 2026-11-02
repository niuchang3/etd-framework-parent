package org.etd.framework.starter.ai.runtime;

import java.util.concurrent.CompletableFuture;
import com.alibaba.cloud.ai.graph.agent.tool.CancellableAsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.CancellationToken;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.ai.agent.ReactAgentExecutor;
import org.etd.framework.starter.ai.memory.AgentMemoryManager;
import org.etd.framework.starter.ai.memory.semantic.AgentKnowledgeQueryService;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.etd.framework.starter.ai.model.client.ModelRequestHeaders;
import org.etd.framework.starter.ai.persistence.spi.AgentRunLeaseStore;
import org.etd.framework.starter.ai.persistence.spi.AgentRunStore;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.framework.starter.ai.runtime.model.AgentChatPreparation;
import org.etd.framework.starter.ai.runtime.model.AgentRunRequest;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEvent;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeResult;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;
import org.etd.framework.starter.ai.tooling.AgentToolAuthorization;
import org.etd.framework.starter.ai.adapter.AgentRunEventListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 真实官方图与生产 Run 容器一起验证授权、可信身份、用量及取消。 */
class AgentSecurityExecutionTest {
    /** 故意携带伪造身份，不能影响线程中的可信租户和用户。 */
    record Input(String query, Long tenantId, Long userId) { }

    @AfterEach
    void clearIdentity() {
        RequestContext.clean();
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void nativeToolLoopKeepsIdentityAndAccumulatesUsageBeforeGraphFiltering() throws Exception {
        identity(1L);
        AtomicInteger rounds = new AtomicInteger();
        AtomicInteger checks = new AtomicInteger();
        AtomicInteger tools = new AtomicInteger();
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> {
            assertIdentity(1L);
            Prompt prompt = invocation.getArgument(0);
            if (rounds.incrementAndGet() == 1) {
                return Flux.deferContextual(context -> {
                    Map<String, String> headers = context.get(ModelRequestHeaders.REACTOR_KEY);
                    assertThat(headers).containsEntry("tenant-code", "1").doesNotContainKey("Authorization");
                    return Flux.just(decision(), usage(100, 2));
                });
            }
            assertThat(prompt.getInstructions()).anyMatch(ToolResponseMessage.class::isInstance);
            return Flux.just(answer("结果"), usage(120, 3), usage(120, 3));
        });
        ToolCallback callback = FunctionToolCallback.builder("lookup", (Input input) -> {
            assertThat(input.tenantId()).isEqualTo(999L);
            assertIdentity(1L);
            tools.incrementAndGet();
            return "真实观察";
        }).description("隔离测试工具").inputType(Input.class).build();
        AgentToolAuthorization policy = name -> {
            assertIdentity(1L);
            checks.incrementAndGet();
        };
        try (Harness harness = new Harness(model, callback, policy)) {
            harness.start();
            assertThat(harness.closed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(harness.result.get().status()).isEqualTo(AgentRuntimeStatus.SUCCEEDED);
            assertThat(harness.result.get().progress().inputTokens()).isEqualTo(220L);
            assertThat(harness.result.get().progress().outputTokens()).isEqualTo(5L);
            assertThat(tools.get()).isEqualTo(1);
            assertThat(checks.get()).isEqualTo(2);
            assertIdentity(1L);
        }
    }

    @Test
    void rejectsPermissionRevokedAfterDiscoveryBeforeExecutingTool() throws Exception {
        identity(1L);
        AtomicInteger checks = new AtomicInteger();
        AtomicInteger tools = new AtomicInteger();
        AtomicInteger modelCalls = new AtomicInteger();
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> {
            modelCalls.incrementAndGet();
            return Flux.just(decision());
        });
        ToolCallback callback = FunctionToolCallback.builder("lookup", (Input input) -> {
            tools.incrementAndGet();
            return "不应执行";
        }).description("授权复核工具").inputType(Input.class).build();
        AgentToolAuthorization policy = name -> {
            assertIdentity(1L);
            if (checks.incrementAndGet() > 1) {
                throw new AccessDeniedException("权限已撤销");
            }
        };
        try (Harness harness = new Harness(model, callback, policy)) {
            harness.start();
            assertThat(harness.closed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(harness.result.get().status()).isEqualTo(AgentRuntimeStatus.FAILED);
            assertThat(modelCalls.get()).isEqualTo(1);
            assertThat(tools.get()).isZero();
        }
    }

    @Test
    void cancellationInterruptsActualOfficialToolThreadAndPersistsCancelledRun() throws Exception {
        identity(1L);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(Prompt.class))).thenReturn(Flux.just(decision()));
        ToolCallback callback = FunctionToolCallback.builder("lookup", (Input input) -> {
            assertIdentity(1L);
            entered.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("未收到取消");
                }
                return "未取消";
            } catch (InterruptedException failure) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
                throw new IllegalStateException("已取消", failure);
            }
        }).description("可中断工具").inputType(Input.class).build();
        AgentToolAuthorization policy = name -> assertIdentity(1L);
        try (Harness harness = new Harness(model, callback, policy)) {
            AgentRunControl control = harness.start();
            try {
                assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
                control.dispose();
                assertThat(interrupted.await(3, TimeUnit.SECONDS)).isTrue();
                assertThat(harness.closed.await(3, TimeUnit.SECONDS)).isTrue();
                assertThat(harness.result.get().status()).isEqualTo(AgentRuntimeStatus.CANCELLED);
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void cancellationReachesOfficialAsyncToolTokenAndFuture() throws Exception {
        identity(1L);
        CountDownLatch entered = new CountDownLatch(1);
        AtomicReference<CancellationToken> received = new AtomicReference<>();
        CompletableFuture<String> pending = new CompletableFuture<>();
        CompletableFuture<Void> releaseFuture = new CompletableFuture<>();
        CountDownLatch cancelled = new CountDownLatch(1);
        pending.whenComplete((value, failure) -> cancelled.countDown());
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(Prompt.class))).thenReturn(Flux.just(decision()));
        ToolDefinition definition = ToolDefinition.builder().name("lookup").description("异步取消测试")
                .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
        ToolCallback callback = new CancellableAsyncToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return definition;
            }

            @Override
            public String call(String arguments) {
                throw new UnsupportedOperationException("使用官方异步调用路径");
            }

            @Override
            public CompletableFuture<String> callAsync(String arguments, ToolContext context, CancellationToken token) {
                assertIdentity(1L);
                received.set(token);
                entered.countDown();
                releaseFuture.join();
                return pending;
            }
        };
        AgentToolAuthorization policy = name -> assertIdentity(1L);
        try (Harness harness = new Harness(model, callback, policy)) {
            AgentRunControl control = harness.start();
            try {
                assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
                control.dispose();
                assertThat(received.get().isCancelled()).isTrue();
                releaseFuture.complete(null);
                assertThat(cancelled.await(3, TimeUnit.SECONDS)).isTrue();
                assertThat(harness.closed.await(3, TimeUnit.SECONDS)).isTrue();
                assertThat(pending.isCancelled()).isTrue();
                assertThat(harness.result.get().status()).isEqualTo(AgentRuntimeStatus.CANCELLED);
            } finally {
                releaseFuture.complete(null);
            }
        }
    }

    @Test
    void failingCancelCallbackStillCancelsFutureAndRestoresForeignCallerContext() throws Exception {
        identity(1L);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch cancelled = new CountDownLatch(1);
        AtomicInteger callbacks = new AtomicInteger();
        CompletableFuture<String> pending = new CompletableFuture<>();
        pending.whenComplete((value, failure) -> cancelled.countDown());
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(Prompt.class))).thenReturn(Flux.just(decision()));
        ToolDefinition definition = ToolDefinition.builder().name("lookup").description("取消回调异常测试")
                .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
        ToolCallback callback = new CancellableAsyncToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return definition;
            }

            @Override
            public String call(String arguments) {
                throw new UnsupportedOperationException("使用官方异步路径");
            }

            @Override
            public CompletableFuture<String> callAsync(String arguments, ToolContext context, CancellationToken token) {
                token.onCancel(this::failCancellation);
                entered.countDown();
                return pending;
            }

            private void failCancellation() {
                assertIdentity(1L);
                callbacks.incrementAndGet();
                throw new IllegalStateException("synthetic-cancel-failure");
            }
        };
        AgentToolAuthorization policy = name -> assertIdentity(1L);
        try (Harness harness = new Harness(model, callback, policy)) {
            AgentRunControl control = harness.start();
            assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
            identity(2L);
            control.dispose();
            assertIdentity(2L);
            assertThat(cancelled.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(harness.closed.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(callbacks.get()).isEqualTo(1);
            assertThat(harness.result.get().status()).isEqualTo(AgentRuntimeStatus.CANCELLED);
        }
    }

    @Test
    void nativeModelCallLimitStopsBeforeSecondDecision() throws Exception {
        verifyNativeLimits(1, 24, 1);
    }

    @Test
    void nativeToolCallLimitStopsBeforeSecondToolExecution() throws Exception {
        verifyNativeLimits(12, 1, 1);
    }

    private void verifyNativeLimits(int modelLimit, int toolLimit, int expectedModelCalls) throws Exception {
        identity(1L);
        AtomicInteger modelCalls = new AtomicInteger();
        AtomicInteger toolCalls = new AtomicInteger();
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> {
            modelCalls.incrementAndGet();
            return Flux.just(decision());
        });
        ToolCallback callback = FunctionToolCallback.builder("lookup", (Input input) -> {
            toolCalls.incrementAndGet();
            return "观察";
        }).description("调用上限测试").inputType(Input.class).build();
        AgentToolAuthorization policy = name -> assertIdentity(1L);
        try (Harness harness = new Harness(model, callback, policy)) {
            harness.properties.setModelCallLimit(modelLimit);
            harness.properties.setToolCallLimit(toolLimit);
            harness.start();
            assertThat(harness.closed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(harness.result.get().status()).isEqualTo(AgentRuntimeStatus.FAILED);
            assertThat(modelCalls.get()).isEqualTo(expectedModelCalls);
            assertThat(toolCalls.get()).isEqualTo(1);
        }
    }

    @Test
    void oversizedToolObservationIsRejectedBeforeNextModelCall() throws Exception {
        identity(1L);
        AtomicInteger modelCalls = new AtomicInteger();
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> {
            modelCalls.incrementAndGet();
            return Flux.just(decision());
        });
        ToolCallback callback = FunctionToolCallback.builder("lookup", (Input input) -> "超长观察".repeat(5000))
                .description("工具结果预算测试").inputType(Input.class).build();
        AgentToolAuthorization policy = name -> assertIdentity(1L);
        try (Harness harness = new Harness(model, callback, policy)) {
            harness.start();
            assertThat(harness.closed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(harness.result.get().status()).isEqualTo(AgentRuntimeStatus.FAILED);
            assertThat(modelCalls.get()).isEqualTo(1);
        }
    }

    @Test
    void refusesBatchToolCallsBeyondExecutionLimitBeforeSecondSideEffect() throws Exception {
        identity(1L);
        AtomicInteger toolCalls = new AtomicInteger();
        ChatModel model = mock(ChatModel.class);
        AssistantMessage.ToolCall first = new AssistantMessage.ToolCall("batch-1", "function", "lookup", "{\"query\":\"资料\"}");
        AssistantMessage.ToolCall second = new AssistantMessage.ToolCall("batch-2", "function", "lookup", "{\"query\":\"资料\"}");
        List<AssistantMessage.ToolCall> calls = List.of(first, second);
        AssistantMessage decision = AssistantMessage.builder().content("").toolCalls(calls).build();
        Generation generation = new Generation(decision);
        ChatResponse batch = new ChatResponse(List.of(generation));
        when(model.stream(any(Prompt.class))).thenReturn(Flux.just(batch));
        ToolCallback callback = FunctionToolCallback.builder("lookup", (Input input) -> {
            toolCalls.incrementAndGet();
            return "观察";
        }).description("批量工具限制").inputType(Input.class).build();
        AgentToolAuthorization policy = name -> assertIdentity(1L);
        try (Harness harness = new Harness(model, callback, policy)) {
            harness.properties.setToolCallLimit(1);
            harness.start();
            assertThat(harness.closed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(harness.result.get().status()).isEqualTo(AgentRuntimeStatus.FAILED);
            assertThat(toolCalls.get()).isEqualTo(1);
        }
    }

    @Test
    void rejectsUndiscoverableToolBeforeAnyModelRequest() throws Exception {
        identity(1L);
        ChatModel model = mock(ChatModel.class);
        AtomicInteger modelCalls = new AtomicInteger();
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> {
            modelCalls.incrementAndGet();
            return Flux.just(answer("不应调用"));
        });
        ToolCallback callback = FunctionToolCallback.builder("lookup", (Input input) -> "不应执行")
                .description("发现权限测试").inputType(Input.class).build();
        AgentToolAuthorization policy = name -> { throw new AccessDeniedException("不可见工具"); };
        try (Harness harness = new Harness(model, callback, policy)) {
            harness.start();
            assertThat(harness.closed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(harness.result.get().status()).isEqualTo(AgentRuntimeStatus.FAILED);
            assertThat(modelCalls.get()).isZero();
        }
    }

    @Test
    void concurrentUsersAndTenantsShareSdkWithoutSharingToolOrMessageIdentity() throws Exception {
        Map<String, AtomicInteger> rounds = new ConcurrentHashMap<>();
        Map<String, AtomicInteger> observations = new ConcurrentHashMap<>();
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> {
            String key = concurrentIdentity();
            AtomicInteger count = rounds.computeIfAbsent(key, ignored -> new AtomicInteger());
            Prompt prompt = invocation.getArgument(0);
            if (count.incrementAndGet() == 1) {
                return Flux.just(decision());
            }
            assertThat(prompt.getInstructions()).anySatisfy(message -> {
                assertThat(message).isInstanceOf(ToolResponseMessage.class);
                ToolResponseMessage result = (ToolResponseMessage) message;
                assertThat(result.getResponses().getFirst().responseData()).contains(key);
            });
            return Flux.just(answer(key));
        });
        ToolCallback callback = FunctionToolCallback.builder("lookup", (Input input) -> {
            String key = concurrentIdentity();
            AtomicInteger count = observations.computeIfAbsent(key, ignored -> new AtomicInteger());
            count.incrementAndGet();
            return key;
        }).description("并发隔离测试").inputType(Input.class).build();
        AgentToolAuthorization policy = name -> concurrentIdentity();
        concurrentIdentity(11L, 1L);
        try (Harness first = new Harness(model, callback, policy);
                Harness second = new Harness(model, callback, policy);
                Harness third = new Harness(model, callback, policy)) {
            first.start();
            concurrentIdentity(12L, 1L);
            second.start();
            concurrentIdentity(21L, 2L);
            third.start();
            assertThat(first.closed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(second.closed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(third.closed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(first.result.get().progress().content()).isEqualTo("1:11");
            assertThat(second.result.get().progress().content()).isEqualTo("1:12");
            assertThat(third.result.get().progress().content()).isEqualTo("2:21");
            assertThat(observations.keySet()).containsExactlyInAnyOrder("1:11", "1:12", "2:21");
        }
    }

    private void concurrentIdentity(Long userId, Long tenantId) {
        RequestContext.clean();
        UserDetails user = new UserDetails();
        user.setId(userId);
        RequestContext.setUser(user);
        RequestContext.setTenantCode(tenantId);
        String key = tenantId + ":" + userId;
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(key, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        MDC.put("trace", key);
    }

    private String concurrentIdentity() {
        String key = RequestContext.getTenantCode() + ":" + RequestContext.getUser().getId();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(key);
        assertThat(MDC.get("trace")).isEqualTo(key);
        return key;
    }

    @Test
    void toolInfrastructureFailureStopsRunBeforeNextModelAndSanitizesPublicError() throws Exception {
        identity(1L);
        AtomicInteger modelCalls = new AtomicInteger();
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> {
            modelCalls.incrementAndGet();
            return Flux.just(decision());
        });
        ToolCallback callback = FunctionToolCallback.builder("lookup", (Input input) -> {
            throw new IllegalStateException("synthetic-provider-secret");
        }).description("工具异常边界测试").inputType(Input.class).build();
        AgentToolAuthorization policy = name -> assertIdentity(1L);
        try (Harness harness = new Harness(model, callback, policy)) {
            harness.start();
            assertThat(harness.closed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(harness.result.get().status()).isEqualTo(AgentRuntimeStatus.FAILED);
            assertThat(harness.result.get().errorMessage()).doesNotContain("synthetic-provider-secret");
            assertThat(modelCalls.get()).isEqualTo(1);
        }
    }

    private void identity(Long tenant) {
        UserDetails user = new UserDetails();
        user.setId(11L);
        RequestContext.setUser(user);
        RequestContext.setTenantCode(tenant);
        RequestContext.setToken("synthetic-token");
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated("user-11", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        MDC.put("trace", "trace-" + tenant);
    }

    private void assertIdentity(Long tenant) {
        assertThat(RequestContext.getTenantCode()).isEqualTo(tenant);
        assertThat(RequestContext.getUser().getId()).isEqualTo(11L);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user-11");
        assertThat(MDC.get("trace")).isEqualTo("trace-" + tenant);
    }

    private ChatResponse decision() {
        AssistantMessage.ToolCall call = new AssistantMessage.ToolCall("call-1", "function", "lookup",
                "{\"query\":\"资料\",\"tenantId\":999,\"userId\":999}");
        List<AssistantMessage.ToolCall> calls = List.of(call);
        AssistantMessage message = AssistantMessage.builder().content("").toolCalls(calls).build();
        Generation generation = new Generation(message);
        return new ChatResponse(List.of(generation));
    }

    private ChatResponse answer(String text) {
        AssistantMessage message = new AssistantMessage(text);
        Generation generation = new Generation(message);
        return new ChatResponse(List.of(generation));
    }

    private ChatResponse usage(int prompt, int completion) {
        DefaultUsage usage = new DefaultUsage(prompt, completion);
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        return new ChatResponse(List.of(), metadata);
    }

    /** 一次 Run 独立资源；生产执行器与容器均不替换，仅模型、授权和存储使用可控替身。 */
    private static final class Harness implements AutoCloseable {
        private final CountDownLatch closed = new CountDownLatch(1);
        private final AtomicReference<AgentRuntimeResult> result = new AtomicReference<>();
        private final AgentTaskRunner runner;
        private final AgentRuntimeProperties properties = new AgentRuntimeProperties();
        private final ReactAgentExecutor executor;
        private final AgentRunRequest request;

        private Harness(ChatModel model, ToolCallback callback, AgentToolAuthorization policy) {
            AgentRunStore store = mock(AgentRunStore.class);
            when(store.finishRun(any())).thenAnswer(invocation -> {
                AgentRuntimeResult actual = invocation.getArgument(0);
                result.set(actual);
                return actual;
            });
            AgentRunLeaseStore leases = mock(AgentRunLeaseStore.class);
            when(leases.renewRunLease(any())).thenReturn(AgentRuntimeStatus.RUNNING);
            ModelClientRegistry models = mock(ModelClientRegistry.class);
            when(models.getChatModel(any(), any())).thenReturn(model);
            DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
            beans.registerSingleton("models", models);
            ToolCallbackProvider provider = () -> new ToolCallback[] {callback};
            beans.registerSingleton("tools", provider);
            AgentMemoryManager memory = new AgentMemoryManager();
            executor = new ReactAgentExecutor(memory, policy, beans.getBeanProvider(ModelClientRegistry.class),
                    beans.getBeanProvider(AgentKnowledgeQueryService.class), beans.getBeanProvider(ToolCallbackProvider.class), properties);
            runner = new AgentTaskRunner(properties, leaseProvider(leases));
            AgentChatPreparation preparation = new AgentChatPreparation("security-run", "1", "100", "系统规则",
                    List.of(), null, null, List.of(), List.of("lookup"), false, null, null, null, null, Map.of());
            AgentRunEventListener listener = new AgentRunEventListener() {
                @Override
                public void onEvent(AgentRuntimeEvent event) {
                }

                @Override
                public void close() {
                    closed.countDown();
                }
            };
            request = new AgentRunRequest(preparation, "问题", store, leases, listener);
        }

        private AgentRunControl start() {
            return runner.start(request, executor);
        }

        @Override
        public void close() {
            runner.close();
        }
    }
    private static ObjectProvider<AgentRunLeaseStore> leaseProvider(AgentRunLeaseStore lease) {
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("lease", lease);
        return beans.getBeanProvider(AgentRunLeaseStore.class);
    }

}
