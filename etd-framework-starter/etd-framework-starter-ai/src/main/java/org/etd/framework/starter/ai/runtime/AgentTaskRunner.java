package org.etd.framework.starter.ai.runtime;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Stream;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.ai.adapter.AgentOutputException;
import org.etd.framework.starter.ai.agent.AgentExecutor;
import org.etd.framework.starter.ai.persistence.spi.AgentRunLeaseStore;
import org.etd.framework.starter.ai.persistence.spi.AgentRunStore;
import org.etd.framework.starter.ai.adapter.AgentRunEventListener;
import org.etd.framework.starter.ai.runtime.model.AgentChatCommand;
import org.etd.framework.starter.ai.runtime.model.AgentChatPreparation;
import org.etd.framework.starter.ai.runtime.spi.AgentChatPrepareService;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot.Scope;
import org.springframework.beans.factory.ObjectProvider;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot;
import org.etd.framework.starter.ai.runtime.model.AgentRunProgress;
import org.etd.framework.starter.ai.runtime.model.AgentRunRequest;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEventType;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeResult;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Signal;

/**
 * 运行与守护容器：纯基础设施层。
 * 负责任务准入、虚拟线程调度、并发限流、超时检测、租约保活、中断取消与上下文传递。
 */
public class AgentTaskRunner implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AgentTaskRunner.class);
    public static final String EXECUTION_FAILED = "AGENT_EXECUTION_FAILED";
    /** 每秒检查本地期限并触发在线维护，不等待上次阻塞操作。 */
    private static final int MAINTENANCE_INTERVAL_SECONDS = 1;
    /** 全局失联回收触发间隔，实际回收最多一个在途操作。 */
    private static final int RECOVERY_INTERVAL_SECONDS = 15;
    /** 主执行和维护执行器共享的优雅关闭等待上限。 */
    private static final int SHUTDOWN_WAIT_SECONDS = 5;

    private final AgentRuntimeProperties properties;
    private final RequestContextSnapshot maintenanceContext = RequestContextSnapshot.maintenance();
    private final ObjectProvider<AgentRunLeaseStore> runStores;
    private final Semaphore capacity;
    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
    /** 定时线程只触发工作，不执行数据库、输出或第三方取消回调。 */
    private final ScheduledThreadPoolExecutor clock;
    /** 每个 Run 每类维护最多一个在途操作，容量在全部操作退出后才归还。 */
    private final ExecutorService maintenanceWorkers = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicBoolean recoveryInFlight = new AtomicBoolean();
    private final Map<String, RunRegistration> activeTasks = new ConcurrentHashMap<>();

    public AgentTaskRunner(AgentRuntimeProperties properties, ObjectProvider<AgentRunLeaseStore> runStores) {
        this.properties = properties;
        this.runStores = runStores;
        this.capacity = new Semaphore(properties.getMaxConcurrentRuns());
        ThreadFactory clockThreads = Thread.ofPlatform().name("agent-maintenance-clock-", 0)
                .inheritInheritableThreadLocals(false).factory();
        this.clock = new ScheduledThreadPoolExecutor(1, clockThreads);
        this.clock.setRemoveOnCancelPolicy(true);
        this.clock.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.clock.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        this.clock.scheduleWithFixedDelay(this::triggerExpiredRunScan,
                RECOVERY_INTERVAL_SECONDS, RECOVERY_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 准入后直接调用业务准备 SPI；不接收 Supplier 或待执行流程。
     * @param command 会话及问题数据 @param output 每次请求独立的输出
     * @param preparation 业务授权及短事务能力 @param store 正文结果存储 @param lease 租约存储
     * @param executor 官方 Agent 执行适配 @return 可取消句柄
     */
    AgentRunControl start(AgentChatCommand command, AgentRunEventListener output,
            AgentChatPrepareService preparation, AgentRunStore store, AgentRunLeaseStore lease, AgentExecutor executor) {
        acquireCapacity();
        AgentRunRequest request = null;
        try {
            AgentChatPreparation prepared = preparation.prepare(command);
            request = new AgentRunRequest(prepared, command.question(), store, lease, output);
            return submitTask(request, executor);
        } catch (RuntimeException failure) {
            failSubmission(request);
            throw failure;
        }
    }

    /** @param request 已准备的明确任务数据 @param executor 执行能力 @return 单次 Run 句柄 */
    AgentRunControl start(AgentRunRequest request, AgentExecutor executor) {
        acquireCapacity();
        try {
            return submitTask(request, executor);
        } catch (RuntimeException failure) {
            failSubmission(request);
            throw failure;
        }
    }

    private void acquireCapacity() {
        if (!capacity.tryAcquire()) {
            throw new ApiRuntimeException("当前生成任务较多，请稍后再试");
        }
    }

    private AgentRunTask submitTask(AgentRunRequest request, AgentExecutor executor) {
        AgentRunTask task = new AgentRunTask(request, properties.getTimeoutSeconds());
        RequestContextSnapshot context = RequestContextSnapshot.capture();
        RunRegistration registration = new RunRegistration(task, context);
        activeTasks.put(task.runId(), registration);
        workers.submit(() -> executeTask(registration, executor));
        return task;
    }

    private void failSubmission(AgentRunRequest request) {
        capacity.release();
        if (request == null) {
            return;
        }
        activeTasks.remove(request.runId());
        AgentRunProgress empty = new AgentRunProgress(request.runId(), "", List.of(), null, null);
        AgentRuntimeResult failed = new AgentRuntimeResult(empty, AgentRuntimeStatus.FAILED,
                EXECUTION_FAILED, "运行任务提交失败，请稍后重试");
        try {
            request.runStore().finishRun(failed);
        } finally {
            request.eventListener().close();
        }
    }

    private void executeTask(RunRegistration registration, AgentExecutor executor) {
        AgentRunTask task = registration.task;
        task.registerWorkerThread();
        ScheduledFuture<?> maintenance = null;
        try (Scope scope = registration.context.openScope()) {
            try {
                maintenance = clock.scheduleWithFixedDelay(() -> triggerMaintenance(registration),
                        MAINTENANCE_INTERVAL_SECONDS, MAINTENANCE_INTERVAL_SECONDS, TimeUnit.SECONDS);
                driveExecution(task, executor);
                finishTask(registration, AgentRuntimeStatus.SUCCEEDED);
            } catch (Exception failure) {
                handleExecutionException(registration, failure);
            } finally {
                cleanupTask(registration, maintenance);
            }
        }
    }

    /** 固定数量的周期任务只做内存检查和受限派发，不等待任何外部服务。 */
    private void triggerMaintenance(RunRegistration registration) {
        AgentRunTask task = registration.task;
        if (!task.isActive()) {
            return;
        }
        if (task.hasTimedOut()) {
            dispatchMaintenance(registration, MaintenanceOperation.STOP);
            return;
        }
        dispatchMaintenance(registration, MaintenanceOperation.LEASE);
        dispatchMaintenance(registration, MaintenanceOperation.HEARTBEAT);
    }

    private void dispatchMaintenance(RunRegistration registration, MaintenanceOperation operation) {
        if (!registration.retainOperation(operation)) {
            return;
        }
        try {
            maintenanceWorkers.submit(() -> executeMaintenance(registration, operation));
        } catch (RejectedExecutionException failure) {
            // 关闭期间不再接收后台工作，也不能把阻塞操作退回定时线程执行。
            completeOperation(registration, operation);
            LOG.debug("Agent维护执行器已关闭，runId={}", registration.task.runId());
        }
    }

    private void executeMaintenance(RunRegistration registration, MaintenanceOperation operation) {
        try (Scope scope = registration.context.openScope()) {
            try {
                if (!registration.registerThread(operation)) {
                    return;
                }
                switch (operation) {
                    case LEASE -> renewLeaseAndSaveProgress(registration);
                    case HEARTBEAT -> sendHeartbeat(registration);
                    case STOP -> stopTask(registration);
                }
            } catch (AgentOutputException failure) {
                registration.task.dispose();
            } catch (Exception failure) {
                registration.task.fail();
                LOG.warn("Agent维护操作异常，runId={}，操作={}，异常类型={}",
                        registration.task.runId(), operation, failure.getClass().getSimpleName());
            }
        } finally {
            completeOperation(registration, operation);
        }
    }

    /** 超时记为失败，容器关闭记为取消；两者的官方取消回调都在本 Run 的上下文内执行。 */
    private void stopTask(RunRegistration registration) {
        if (registration.shutdownRequested) {
            registration.task.dispose();
        } else {
            registration.task.fail();
        }
    }

    private void driveExecution(AgentRunTask task, AgentExecutor executor) {
        task.output().emit(AgentRuntimeEventType.START, null);
        task.requireActive();
        Flux<String> stream = executor.execute(task.request().preparation(), task.request().question(), task);
        stream = stream.takeUntilOther(task.stopSignal());
        consumeResponseStream(task, stream);
        if (!task.output().hasContent() && task.isActive()) {
            throw new IllegalStateException("模型未返回可展示正文");
        }
    }

    private void consumeResponseStream(AgentRunTask task, Flux<String> stream) {
        try (Stream<Signal<String>> responses = stream.materialize().toStream(1)) {
            Iterator<Signal<String>> iterator = responses.iterator();
            while (iterator.hasNext()) {
                task.requireActive();
                Signal<String> signal = iterator.next();
                if (signal.isOnError()) {
                    throw new IllegalStateException("模型调用流发生错误");
                }
                if (signal.hasValue()) {
                    String text = signal.get();
                    task.output().append(text, properties.getMaxResponseCharacters());
                    task.output().emit(AgentRuntimeEventType.DELTA, text);
                }
            }
        }
    }

    /** 数据库维护独占本 Run 的数据库通道，慢心跳不会阻止续租或读取跨节点取消状态。 */
    private void renewLeaseAndSaveProgress(RunRegistration registration) {
        AgentRunTask task = registration.task;
        if (!task.isActive()) {
            return;
        }
        AgentRuntimeStatus status = task.request().leaseStore().renewRunLease(task.runId());
        if (status != AgentRuntimeStatus.RUNNING && status != AgentRuntimeStatus.SUCCEEDED) {
            task.stop(status);
            return;
        }
        if (!task.isActive()) {
            return;
        }
        long now = System.nanoTime();
        if (now - registration.lastProgressSaveNanos >= TimeUnit.MILLISECONDS.toNanos(properties.getCheckpointMillis())) {
            AgentRunProgress progress = task.output().progress();
            task.request().runStore().saveRunProgress(progress);
            registration.lastProgressSaveNanos = now;
        }
    }

    /** 心跳单独执行；同一 Run 的网络阻塞不会排队产生更多心跳任务。 */
    private void sendHeartbeat(RunRegistration registration) {
        if (!registration.task.isActive()) {
            return;
        }
        long now = System.nanoTime();
        if (now - registration.lastHeartbeatNanos >= TimeUnit.SECONDS.toNanos(properties.getHeartbeatSeconds())) {
            registration.task.output().emit(AgentRuntimeEventType.HEARTBEAT, null);
            registration.lastHeartbeatNanos = now;
        }
    }

    private void finishTask(RunRegistration registration, AgentRuntimeStatus fallback) {
        AgentRunTask task = registration.task;
        // 关闭触发与工作线程启动可能竞争，关闭请求本身也应参与终态裁决。
        AgentRuntimeStatus defaultStatus = registration.shutdownRequested ? AgentRuntimeStatus.CANCELLED : fallback;
        AgentRuntimeStatus desired = task.beginFinishing(defaultStatus);
        registration.stopMaintenance();
        // 停止阶段的中断已完成传播，进入短事务前清除工作线程的中断标记。
        Thread.interrupted();
        AgentRunProgress progress = task.output().progress();
        String code = desired == AgentRuntimeStatus.FAILED ? EXECUTION_FAILED : null;
        String message = desired == AgentRuntimeStatus.FAILED ? "回答生成失败或超时，请检查模型与服务配置后重试" : null;
        AgentRuntimeResult result = new AgentRuntimeResult(progress, desired, code, message);
        AgentRuntimeResult actual = task.request().runStore().finishRun(result);
        task.markPersisted();
        task.output().emitTerminal(actual);
    }

    private void handleExecutionException(RunRegistration registration, Exception ex) {
        AgentRunTask task = registration.task;
        LOG.warn("Agent任务执行异常，runId={}，异常类型={}", task.runId(), ex.getClass().getSimpleName());
        Thread.interrupted();
        if (ex instanceof AgentOutputException) {
            task.dispose();
        }
        if (task.needsPersistence()) {
            finishTask(registration, AgentRuntimeStatus.FAILED);
        }
    }

    private void cleanupTask(RunRegistration registration, ScheduledFuture<?> maintenance) {
        registration.stopMaintenance();
        registration.task.markClosed();
        if (maintenance != null) {
            maintenance.cancel(false);
        }
        try {
            registration.task.output().close();
        } finally {
            if (registration.releaseExecution()) {
                releaseCapacity(registration);
            }
        }
    }

    /** 主执行和全部维护均已退出才归还容量，不为不响应中断的底层调用无限创建替代线程。 */
    private void completeOperation(RunRegistration registration, MaintenanceOperation operation) {
        if (registration.releaseOperation(operation)) {
            releaseCapacity(registration);
        }
    }

    private void releaseCapacity(RunRegistration registration) {
        activeTasks.remove(registration.task.runId(), registration);
        capacity.release();
    }

    /** 全局失联回收最多一个在途任务，与在线 Run 的数据库及心跳操作不共享工作线程。 */
    void triggerExpiredRunScan() {
        if (!recoveryInFlight.compareAndSet(false, true)) {
            return;
        }
        try {
            maintenanceWorkers.submit(this::recoverExpiredRuns);
        } catch (RejectedExecutionException failure) {
            recoveryInFlight.set(false);
        }
    }

    private void recoverExpiredRuns() {
        try {
            scanExpiredRuns();
        } finally {
            recoveryInFlight.set(false);
        }
    }

    /** 全局维护不继承任何请求身份；仅调用有限的过期租约领取与修复接口。 */
    void scanExpiredRuns() {
        try (Scope scope = maintenanceContext.openScope()) {
            for (AgentRunLeaseStore store : runStores) {
                try {
                    store.finishExpiredRunList();
                } catch (Exception failure) {
                    LOG.warn("扫描并回收过期Agent运行任务异常，异常类型={}", failure.getClass().getSimpleName());
                }
            }
        }
    }

    @Override
    public void close() {
        clock.shutdownNow();
        for (RunRegistration registration : activeTasks.values()) {
            registration.shutdownRequested = true;
            registration.task.output().disconnect();
            dispatchMaintenance(registration, MaintenanceOperation.STOP);
        }
        workers.shutdown();
        maintenanceWorkers.shutdown();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(SHUTDOWN_WAIT_SECONDS);
        try {
            workers.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS);
            long remaining = Math.max(0, deadline - System.nanoTime());
            maintenanceWorkers.awaitTermination(remaining, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        workers.shutdownNow();
        maintenanceWorkers.shutdownNow();
    }

    /** 每类操作独立占位，数据库阻塞、网络阻塞和第三方取消回调不会互占固定调度线程。 */
    private enum MaintenanceOperation {
        /** 续租、跨节点取消状态查询及部分正文保存。 */
        LEASE,
        /** 仅发送客户端心跳。 */
        HEARTBEAT,
        /** 超时或关闭时发出停止信号，可能同步调用第三方取消回调。 */
        STOP
    }

    /**
     * 本 Run 的维护占位与资源计数，不保存待执行流程。
     * 对象锁保护登记、退出和线程中断；每类最多一个在途操作，工作线程另占一个引用。
     */
    private static final class RunRegistration {
        private final AgentRunTask task;
        private final RequestContextSnapshot context;
        private final EnumSet<MaintenanceOperation> inFlight = EnumSet.noneOf(MaintenanceOperation.class);
        private final EnumMap<MaintenanceOperation, Thread> threads = new EnumMap<>(MaintenanceOperation.class);
        private boolean closing;
        private volatile boolean shutdownRequested;
        private int operations = 1;
        /** 分别只由本 Run 的心跳、数据库维护通道更新，通道退出与重入通过对象锁发布。 */
        private long lastHeartbeatNanos = System.nanoTime();
        private long lastProgressSaveNanos = lastHeartbeatNanos;

        private RunRegistration(AgentRunTask task, RequestContextSnapshot context) {
            this.task = task;
            this.context = context;
        }

        private synchronized boolean retainOperation(MaintenanceOperation operation) {
            if (closing || !inFlight.add(operation)) {
                return false;
            }
            operations++;
            return true;
        }

        private synchronized boolean registerThread(MaintenanceOperation operation) {
            if (closing && operation != MaintenanceOperation.STOP) {
                return false;
            }
            threads.put(operation, Thread.currentThread());
            return true;
        }

        private synchronized boolean releaseOperation(MaintenanceOperation operation) {
            threads.remove(operation);
            inFlight.remove(operation);
            return --operations == 0;
        }

        private synchronized boolean releaseExecution() {
            return --operations == 0;
        }

        /** 不等待数据库或网络退出；在途操作仍占容量，直到它们真正返回。 */
        private synchronized void stopMaintenance() {
            closing = true;
            for (Map.Entry<MaintenanceOperation, Thread> entry : threads.entrySet()) {
                if (entry.getKey() != MaintenanceOperation.STOP && entry.getValue() != Thread.currentThread()) {
                    entry.getValue().interrupt();
                }
            }
        }
    }
}
