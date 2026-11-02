package org.etd.framework.starter.ai.runtime;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.UserDetails;
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
import org.mockito.ArgumentCaptor;
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

    /** 两个续租操作同时阻塞时，第三个 Run 的超时守护不能被它们占住。 */
    @Test
    void blockedLeaseOperationsDoNotDelayAnotherRunsDeadline() throws Exception {
        taskRunner.close();
        properties.setMaxConcurrentRuns(3);
        properties.setTimeoutSeconds(2);
        taskRunner = new AgentTaskRunner(properties, leaseProvider(leaseStore));
        CountDownLatch blocked = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        when(leaseStore.renewRunLease(any())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            if (id.startsWith("blocked-")) {
                awaitBlockedMaintenance(blocked, release);
            }
            return AgentRuntimeStatus.RUNNING;
        });
        CountDownLatch closed = new CountDownLatch(1);
        try {
            startNeverEndingRun("blocked-1", closingListener(new CountDownLatch(1), false));
            startNeverEndingRun("blocked-2", closingListener(new CountDownLatch(1), false));
            assertThat(blocked.await(3, TimeUnit.SECONDS)).isTrue();
            startNeverEndingRun("independent-deadline", closingListener(closed, false));
            assertThat(closed.await(4, TimeUnit.SECONDS)).isTrue();
        } finally {
            release.countDown();
        }
    }

    /** 模拟驱动不响应线程中断；只由测试放行，避免假设数据库一定可被中断。 */
    private void awaitBlockedMaintenance(CountDownLatch entered, CountDownLatch release) {
        entered.countDown();
        boolean interrupted = false;
        while (release.getCount() != 0) {
            try {
                release.await();
            } catch (InterruptedException failure) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private AgentRunControl startNeverEndingRun(String id, AgentRunEventListener output) {
        AgentChatPreparation preparation = prepared(id);
        AgentRunRequest request = new AgentRunRequest(preparation, "问题", runStore, leaseStore, output);
        AgentExecutor executor = (prepared, question, control) -> Flux.never();
        return taskRunner.start(request, executor);
    }

    /** 慢心跳不能拖住同一 Run 的续租及跨节点取消查询。 */
    @Test
    void blockedHeartbeatDoesNotDelayLeaseCancellation() throws Exception {
        CountDownLatch heartbeat = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch persisted = new CountDownLatch(1);
        AtomicInteger renewals = new AtomicInteger();
        when(leaseStore.renewRunLease(any())).thenAnswer(invocation -> {
            int calls = renewals.incrementAndGet();
            return calls > 1 && heartbeat.getCount() == 0 ? AgentRuntimeStatus.CANCELLED : AgentRuntimeStatus.RUNNING;
        });
        when(runStore.finishRun(any())).thenAnswer(invocation -> {
            AgentRuntimeResult result = invocation.getArgument(0);
            assertThat(result.status()).isEqualTo(AgentRuntimeStatus.CANCELLED);
            persisted.countDown();
            return result;
        });
        AgentRunEventListener output = new AgentRunEventListener() {
            @Override
            public void onEvent(AgentRuntimeEvent event) {
                if (event.type() == AgentRuntimeEventType.HEARTBEAT) {
                    awaitBlockedMaintenance(heartbeat, release);
                }
            }
        };
        try {
            startNeverEndingRun("slow-heartbeat", output);
            assertThat(heartbeat.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(persisted.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(renewals.get()).isGreaterThanOrEqualTo(2);
        } finally {
            release.countDown();
        }
    }

    /** 底层维护不响应中断时，Run 退出也不能提前归还容量并不断创建替代维护线程。 */
    @Test
    void blockedMaintenanceRetainsCapacityAndDoesNotOverlap() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch closed = new CountDownLatch(1);
        AtomicInteger renewals = new AtomicInteger();
        when(leaseStore.renewRunLease(any())).thenAnswer(invocation -> {
            renewals.incrementAndGet();
            awaitBlockedMaintenance(entered, release);
            return AgentRuntimeStatus.RUNNING;
        });
        AgentRunControl control = startNeverEndingRun("retained-maintenance", closingListener(closed, false));
        try {
            assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
            control.dispose();
            assertThat(closed.await(3, TimeUnit.SECONDS)).isTrue();
            startNeverEndingRun("remaining-slot", closingListener(new CountDownLatch(1), false));
            AgentRunEventListener third = closingListener(new CountDownLatch(1), false);
            assertThatThrownBy(() -> startNeverEndingRun("over-capacity", third))
                    .isInstanceOf(ApiRuntimeException.class);
            // 第一个 Run 的数据库调用未退出，不能为它重复启动续租任务。
            assertThat(renewals.get()).isEqualTo(1);
        } finally {
            release.countDown();
        }
    }

    /** 高频短任务取消周期维护后，队列应只剩全局扫描触发器，而非等待延迟到期才移除。 */
    @Test
    void completedRunsImmediatelyRemoveTheirScheduledTasks() throws Exception {
        ScheduledThreadPoolExecutor clock = (ScheduledThreadPoolExecutor) ReflectionTestUtils.getField(taskRunner, "clock");
        AgentExecutor executor = (prepared, question, control) -> Flux.just("完成");
        for (int index = 0; index < 20; index++) {
            CountDownLatch closed = new CountDownLatch(1);
            AgentRunEventListener output = closingListener(closed, false);
            AgentChatPreparation preparation = prepared("short-" + index);
            AgentRunRequest request = new AgentRunRequest(preparation, "问题", runStore, leaseStore, output);
            taskRunner.start(request, executor);
            assertThat(closed.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(clock.getQueue()).hasSize(1);
        }
    }

    /** 全局回收阻塞时只允许一个在途任务，不能阻塞在线任务的超时守护。 */
    @Test
    void blockedRecoveryDoesNotDelayOnlineDeadlineOrDuplicateRecovery() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger recoveries = new AtomicInteger();
        doAnswer(invocation -> {
            recoveries.incrementAndGet();
            awaitBlockedMaintenance(entered, release);
            return null;
        }).when(leaseStore).finishExpiredRunList();
        properties.setTimeoutSeconds(1);
        CountDownLatch closed = new CountDownLatch(1);
        try {
            taskRunner.triggerExpiredRunScan();
            assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
            taskRunner.triggerExpiredRunScan();
            startNeverEndingRun("deadline-during-recovery", closingListener(closed, false));
            assertThat(closed.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(recoveries.get()).isEqualTo(1);
        } finally {
            release.countDown();
        }
    }

    /** 官方取消令牌同步执行第三方回调时，也不能占住调度线程影响另一个 Run。 */
    @Test
    void blockingCancellationCallbackDoesNotDelayAnotherDeadline() throws Exception {
        properties.setTimeoutSeconds(1);
        CountDownLatch callback = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch firstClosed = new CountDownLatch(1);
        CountDownLatch secondClosed = new CountDownLatch(1);
        AgentChatPreparation preparation = prepared("blocking-cancel-callback");
        AgentRunEventListener output = closingListener(firstClosed, false);
        AgentRunRequest request = new AgentRunRequest(preparation, "问题", runStore, leaseStore, output);
        AgentExecutor executor = (prepared, question, control) -> configureBlockingCancellation(control, callback, release);
        try {
            taskRunner.start(request, executor);
            assertThat(callback.await(3, TimeUnit.SECONDS)).isTrue();
            startNeverEndingRun("other-deadline", closingListener(secondClosed, false));
            assertThat(secondClosed.await(3, TimeUnit.SECONDS)).isTrue();
        } finally {
            release.countDown();
        }
        assertThat(firstClosed.await(3, TimeUnit.SECONDS)).isTrue();
    }

    /** 新的数据库、心跳和停止异步边界必须使用 Run 快照，不能使用触发线程的身份。 */
    @Test
    void backgroundMaintenanceAndTimeoutKeepRunIdentity() throws Exception {
        UserDetails user = new UserDetails();
        user.setId(1701L);
        RequestContext.setUser(user);
        RequestContext.setTenantCode(17L);
        RequestContext.setOrganizationId(170L);
        RequestContext.setAttribute("x-business", "maintenance-business");
        RequestContext.setToken("synthetic-run-maintenance");
        MDC.put("trace", "maintenance-17");
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated("user-17", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        properties.setTimeoutSeconds(2);
        CountDownLatch renewed = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);
        CountDownLatch closed = new CountDownLatch(1);
        when(leaseStore.renewRunLease(any())).thenAnswer(invocation -> {
            assertMaintenanceIdentity(authentication);
            renewed.countDown();
            return AgentRuntimeStatus.RUNNING;
        });
        AgentChatPreparation preparation = prepared("maintenance-context");
        AgentRunEventListener output = identityCheckingListener(authentication, closed);
        AgentRunRequest request = new AgentRunRequest(preparation, "问题", runStore, leaseStore, output);
        AgentExecutor executor = (prepared, question, control) -> configureIdentityCancellation(control, authentication, stopped);
        try {
            taskRunner.start(request, executor);
            RequestContext.clean();
            SecurityContextHolder.clearContext();
            MDC.clear();
            assertThat(renewed.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(stopped.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(closed.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(RequestContext.getTenantCode()).isNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        } finally {
            RequestContext.clean();
            SecurityContextHolder.clearContext();
            MDC.clear();
        }
    }

    private void assertStopIdentity(UsernamePasswordAuthenticationToken authentication, CountDownLatch stopped) {
        assertMaintenanceIdentity(authentication);
        stopped.countDown();
    }

    /** 官方取消令牌要求回调签名，回调只调用明确的阻塞测试方法。 */
    private Flux<String> configureBlockingCancellation(AgentRunControl control, CountDownLatch callback, CountDownLatch release) {
        control.cancellationToken().onCancel(() -> awaitBlockedMaintenance(callback, release));
        return Flux.never();
    }

    /** 测试只通过官方令牌注册，取消回调的线程由生产容器决定。 */
    private Flux<String> configureIdentityCancellation(AgentRunControl control,
            UsernamePasswordAuthenticationToken authentication, CountDownLatch stopped) {
        control.cancellationToken().onCancel(() -> assertStopIdentity(authentication, stopped));
        return Flux.never();
    }

    private void assertMaintenanceIdentity(UsernamePasswordAuthenticationToken authentication) {
        assertThat(RequestContext.getTenantCode()).isEqualTo(17L);
        assertThat(RequestContext.getUser().getId()).isEqualTo(1701L);
        assertThat(RequestContext.getOrganizationId()).isEqualTo(170L);
        assertThat(RequestContext.getHeaderContext().toMap()).containsEntry("x-business", "maintenance-business");
        assertThat(RequestContext.getToken()).isEqualTo("synthetic-run-maintenance");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
        assertThat(MDC.get("trace")).isEqualTo("maintenance-17");
    }

    /** 开始、后台心跳、终态和关闭都必须恢复同一 Run 的完整请求上下文。 */
    private AgentRunEventListener identityCheckingListener(UsernamePasswordAuthenticationToken authentication,
            CountDownLatch closed) {
        return new AgentRunEventListener() {
            @Override
            public void onEvent(AgentRuntimeEvent event) {
                assertMaintenanceIdentity(authentication);
            }

            @Override
            public void close() {
                assertMaintenanceIdentity(authentication);
                closed.countDown();
            }
        };
    }

    /** 容器关闭属于取消，不因关闭定时器或异步派发竞争而错误保存成模型失败。 */
    @Test
    void shutdownPersistsCancellation() throws Exception {
        CountDownLatch closed = new CountDownLatch(1);
        AgentRunEventListener output = closingListener(closed, false);
        startNeverEndingRun("shutdown-cancel", output);
        taskRunner.close();
        assertThat(closed.await(3, TimeUnit.SECONDS)).isTrue();
        ArgumentCaptor<AgentRuntimeResult> result = ArgumentCaptor.forClass(AgentRuntimeResult.class);
        verify(runStore).finishRun(result.capture());
        assertThat(result.getValue().status()).isEqualTo(AgentRuntimeStatus.CANCELLED);
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
