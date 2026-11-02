package org.etd.framework.starter.ai.runtime;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.ai.adapter.AgentRunEventListener;
import org.etd.framework.starter.ai.adapter.AgentOutputException;
import org.etd.framework.starter.ai.agent.AgentExecutor;
import org.etd.framework.starter.ai.persistence.spi.AgentRunStore;
import org.etd.framework.starter.ai.persistence.spi.AgentRunLeaseStore;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.framework.starter.ai.runtime.model.AgentChatPreparation;
import org.etd.framework.starter.ai.runtime.model.AgentRunRequest;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEvent;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEventType;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeResult;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.etd.framework.common.core.context.model.RequestContext;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("AgentTaskRunner 运行容器测试")
class AgentTaskRunnerTest {

    private AgentRuntimeProperties properties;
    private AgentTaskRunner taskRunner;
    private AgentRunStore runStore;
    private AgentRunLeaseStore leaseStore;

    @BeforeEach
    void setUp() {
        properties = new AgentRuntimeProperties();
        properties.setMaxConcurrentRuns(2);
        properties.setTimeoutSeconds(5);
        properties.setMaxResponseCharacters(5000);
        properties.setHeartbeatSeconds(1);
        properties.setCheckpointMillis(200);

        runStore = mock(AgentRunStore.class);
        leaseStore = mock(AgentRunLeaseStore.class);
        when(leaseStore.renewRunLease(any())).thenReturn(AgentRuntimeStatus.RUNNING);
        when(runStore.finishRun(any())).thenAnswer(invocation -> invocation.getArgument(0));

        taskRunner = new AgentTaskRunner(properties, leaseProvider(leaseStore));
    }

    @AfterEach
    void tearDown() {
        taskRunner.close();
    }

    @Test
    @DisplayName("验证虚拟线程驱动与正常流式消费完成")
    void shouldExecuteAgentTaskSuccessfully() throws Exception {
        ChatModel model = mock(ChatModel.class);
        CountDownLatch latch = new CountDownLatch(1);
        List<AgentRuntimeEvent> events = new ArrayList<>();

        AgentRunEventListener listener = new AgentRunEventListener() {
            @Override
            public void onEvent(AgentRuntimeEvent event) {
                events.add(event);
                if (event.type() == AgentRuntimeEventType.TERMINAL) {
                    latch.countDown();
                }
            }
        };

        AgentChatPreparation preparation = prepared("run-101");
        AgentRunRequest runRequest = new AgentRunRequest(preparation, "Hello", runStore, leaseStore, listener);

        AgentExecutor executor = (m, req, ctrl) -> {
            ChatResponse r1 = new ChatResponse(List.of(new Generation(new AssistantMessage("Hello,"))));
            ChatResponse r2 = new ChatResponse(List.of(new Generation(new AssistantMessage(" world!"))));
            return Flux.just("Hello,", " world!");
        };

        AgentRunControl control = taskRunner.start(runRequest, executor);
        assertThat(control.runId()).isEqualTo("run-101");

        boolean finished = latch.await(3, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        List<AgentRuntimeEventType> types = events.stream().map(AgentRuntimeEvent::type).toList();
        assertThat(types).contains(AgentRuntimeEventType.START, AgentRuntimeEventType.DELTA, AgentRuntimeEventType.TERMINAL);
    }

    @Test
    @DisplayName("超出并发容量限制时立即拒绝，防止任务堆积")
    void shouldRejectWhenExceedingCapacity() throws Exception {
        CountDownLatch blockLatch = new CountDownLatch(1);
        CountDownLatch startedLatch = new CountDownLatch(2);

        // 占满 2 个容量
        for (int i = 0; i < 2; i++) {
            String id = "task-" + i;
            AgentChatPreparation preparation = prepared(id);
            AgentRunEventListener output = event -> { };
            AgentRunRequest runReq = new AgentRunRequest(preparation, "Q", runStore, leaseStore, output);
            taskRunner.start(runReq, (m, r, c) -> {
                startedLatch.countDown();
                return Flux.never();
            });
        }

        boolean allStarted = startedLatch.await(2, TimeUnit.SECONDS);
        assertThat(allStarted).isTrue();

        // 提交第 3 个任务，预期触发限流拒绝
        AgentChatPreparation third = prepared("task-overflow");
        AgentRunEventListener output = event -> { };
        AgentRunRequest thirdRunReq = new AgentRunRequest(third, "Q", runStore, leaseStore, output);

        assertThatThrownBy(() -> taskRunner.start(thirdRunReq, (m, r, c) -> Flux.empty()))
                .isInstanceOf(ApiRuntimeException.class)
                .hasMessageContaining("当前生成任务较多");
    }

    @Test
    @DisplayName("外部调用 cancel 应及时中止任务并向存储提交取消终态")
    void shouldCancelRunningTask() throws Exception {
        ChatModel model = mock(ChatModel.class);
        CountDownLatch closedLatch = new CountDownLatch(1);

        AgentRunEventListener listener = new AgentRunEventListener() {
            @Override
            public void onEvent(AgentRuntimeEvent event) {
            }

            @Override
            public void close() {
                closedLatch.countDown();
            }
        };

        AgentChatPreparation preparation = prepared("run-cancel");
        AgentRunRequest runRequest = new AgentRunRequest(preparation, "Infinite", runStore, leaseStore, listener);

        AgentExecutor executor = (m, req, ctrl) -> Flux.interval(java.time.Duration.ofMillis(100))
                .map(i -> "chunk " + i);

        AgentRunControl control = taskRunner.start(runRequest, executor);
        Thread.sleep(250);
        control.dispose();

        boolean closed = closedLatch.await(3, TimeUnit.SECONDS);
        assertThat(closed).isTrue();

        org.mockito.ArgumentCaptor<AgentRuntimeResult> captor = org.mockito.ArgumentCaptor.forClass(AgentRuntimeResult.class);
        org.mockito.Mockito.verify(runStore).finishRun(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(AgentRuntimeStatus.CANCELLED);
    }

    @Test
    void repeatedDisposalDoesNotInterruptTerminalPersistence() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch closed = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        when(runStore.finishRun(any())).thenAnswer(invocation -> persistUntilReleased(invocation.getArgument(0), entered, release, interrupted));
        AgentRunEventListener output = new AgentRunEventListener() {
            @Override
            public void onEvent(AgentRuntimeEvent event) {
            }

            @Override
            public void close() {
                closed.countDown();
            }
        };
        AgentChatPreparation preparation = prepared("repeated-dispose");
        AgentRunRequest request = new AgentRunRequest(preparation, "问题", runStore, leaseStore, output);
        AgentExecutor executor = (prepared, question, control) -> Flux.never();
        AgentRunControl control = taskRunner.start(request, executor);
        try {
            control.dispose();
            assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
            control.dispose();
            assertThat(control.isDisposed()).isTrue();
        } finally {
            release.countDown();
        }
        assertThat(closed.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(interrupted.get()).isFalse();
    }

    private AgentRuntimeResult persistUntilReleased(AgentRuntimeResult result, CountDownLatch entered,
            CountDownLatch release, AtomicBoolean interrupted) throws InterruptedException {
        entered.countDown();
        try {
            if (!release.await(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException("测试未放行终态存储");
            }
            interrupted.set(Thread.currentThread().isInterrupted());
            return result;
        } catch (InterruptedException failure) {
            interrupted.set(true);
            throw failure;
        }
    }

    /** 首次取消恰逢成功结果提交时，也不能中断事务；数据库仍负责裁决真实终态。 */
    @Test
    void firstDisposalDuringTerminalPersistenceDoesNotInterruptCommit() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch closed = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        when(runStore.finishRun(any())).thenAnswer(invocation -> persistUntilReleased(invocation.getArgument(0), entered, release, interrupted));
        AgentRunEventListener output = closingListener(closed, false);
        AgentChatPreparation preparation = prepared("dispose-during-commit");
        AgentRunRequest request = new AgentRunRequest(preparation, "问题", runStore, leaseStore, output);
        AgentExecutor executor = (prepared, question, control) -> Flux.just("已生成正文");
        AgentRunControl control = taskRunner.start(request, executor);
        try {
            assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
            control.dispose();
            assertThat(control.isDisposed()).isTrue();
        } finally {
            release.countDown();
        }
        assertThat(closed.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(interrupted.get()).isFalse();
        verify(runStore, times(1)).finishRun(any());
    }

    /** 终态发送失败只关闭输出，不能把已经保存的成功结果重复提交为失败。 */
    @Test
    void terminalOutputFailureDoesNotRepeatPersistence() throws Exception {
        CountDownLatch closed = new CountDownLatch(1);
        AgentRunEventListener output = closingListener(closed, true);
        AgentChatPreparation preparation = prepared("terminal-output-failure");
        AgentRunRequest request = new AgentRunRequest(preparation, "问题", runStore, leaseStore, output);
        AgentExecutor executor = (prepared, question, control) -> Flux.just("已生成正文");
        taskRunner.start(request, executor);
        assertThat(closed.await(3, TimeUnit.SECONDS)).isTrue();
        verify(runStore, times(1)).finishRun(any());
    }

    /** 通过真实事件接口控制终态失败，并以关闭信号等待整个收尾流程结束。 */
    private AgentRunEventListener closingListener(CountDownLatch closed, boolean failTerminal) {
        return new AgentRunEventListener() {
            @Override
            public void onEvent(AgentRuntimeEvent event) {
                if (failTerminal && event.type() == AgentRuntimeEventType.TERMINAL) {
                    throw new AgentOutputException();
                }
            }

            @Override
            public void close() {
                closed.countDown();
            }
        };
    }

    @Test
    void maintenanceHasNoRequestPrivilegesAndRestoresCallerEvenOnStoreFailure() {
        RequestContext.setTenantCode(9L);
        RequestContext.setToken("synthetic-maintenance-test");
        MDC.put("trace", "caller");
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated("caller", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        doAnswer(invocation -> {
            assertThat(RequestContext.getTenantCode()).isNull();
            assertThat(RequestContext.getUser()).isNull();
            assertThat(RequestContext.getToken()).isNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(MDC.get("trace")).isNull();
            throw new IllegalStateException("synthetic-maintenance-failure");
        }).when(leaseStore).finishExpiredRunList();
        try {
            taskRunner.scanExpiredRuns();
            assertThat(RequestContext.getTenantCode()).isEqualTo(9L);
            assertThat(RequestContext.getToken()).isEqualTo("synthetic-maintenance-test");
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
            assertThat(MDC.get("trace")).isEqualTo("caller");
        } finally {
            RequestContext.clean();
            SecurityContextHolder.clearContext();
            MDC.clear();
        }
    }

    private AgentChatPreparation prepared(String id) {
        return new AgentChatPreparation(id, "tenant", "model", "System", List.of(), null, null,
                List.of(), List.of(), false, null, null, null, null, Map.of());
    }

    private static ObjectProvider<AgentRunLeaseStore> leaseProvider(AgentRunLeaseStore lease) {
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("lease", lease);
        return beans.getBeanProvider(AgentRunLeaseStore.class);
    }

}
