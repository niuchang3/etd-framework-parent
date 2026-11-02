package org.etd.framework.starter.ai.runtime;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
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

    private final AgentRuntimeProperties properties;
    private final RequestContextSnapshot maintenanceContext = RequestContextSnapshot.maintenance();
    private final ObjectProvider<AgentRunLeaseStore> runStores;
    private final Semaphore capacity;
    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
    private final ScheduledExecutorService clock = Executors.newScheduledThreadPool(2);
    private final Map<String, AgentRunTask> activeTasks = new ConcurrentHashMap<>();

    public AgentTaskRunner(AgentRuntimeProperties properties, ObjectProvider<AgentRunLeaseStore> runStores) {
        this.properties = properties;
        this.runStores = runStores;
        this.capacity = new Semaphore(properties.getMaxConcurrentRuns());
        this.clock.scheduleWithFixedDelay(this::scanExpiredRuns, 15, 15, TimeUnit.SECONDS);
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
        activeTasks.put(task.runId(), task);
        workers.submit(() -> executeTask(task, executor, context));
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

    private void executeTask(AgentRunTask task, AgentExecutor executor, RequestContextSnapshot context) {
        task.registerWorkerThread();
        MaintenanceTimes times = new MaintenanceTimes();
        ScheduledFuture<?> maintenance = null;
        try (Scope scope = context.openScope()) {
            try {
                maintenance = clock.scheduleWithFixedDelay(() -> maintainInContext(task, context, times), 1, 1, TimeUnit.SECONDS);
                driveExecution(task, executor);
                finishTask(task, AgentRuntimeStatus.SUCCEEDED);
            } catch (Exception failure) {
                handleExecutionException(task, failure);
            } finally {
                cleanupTask(task, maintenance);
            }
        }
    }

    private void maintainInContext(AgentRunTask task, RequestContextSnapshot context, MaintenanceTimes times) {
        try (Scope scope = context.openScope()) {
            maintainTask(task, times);
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

    private void maintainTask(AgentRunTask task, MaintenanceTimes times) {
        if (!task.isActive()) {
            return;
        }
        try {
            if (task.hasTimedOut()) {
                task.stop(AgentRuntimeStatus.FAILED);
                return;
            }
            AgentRuntimeStatus status = task.request().leaseStore().renewRunLease(task.runId());
            if (status != AgentRuntimeStatus.RUNNING && status != AgentRuntimeStatus.SUCCEEDED) {
                task.stop(status);
                return;
            }
            heartbeatAndSaveProgress(task, times);
        } catch (AgentOutputException ex) {
            task.dispose();
        } catch (Exception ex) {
            task.stop(AgentRuntimeStatus.FAILED);
        }
    }

    private void heartbeatAndSaveProgress(AgentRunTask task, MaintenanceTimes times) {
        long now = System.nanoTime();
        if (now - times.lastHeartbeatNanos >= TimeUnit.SECONDS.toNanos(properties.getHeartbeatSeconds())) {
            task.output().emit(AgentRuntimeEventType.HEARTBEAT, null);
            times.lastHeartbeatNanos = now;
        }
        if (now - times.lastProgressSaveNanos >= TimeUnit.MILLISECONDS.toNanos(properties.getCheckpointMillis())) {
            AgentRunProgress progress = task.output().progress();
            task.request().runStore().saveRunProgress(progress);
            times.lastProgressSaveNanos = now;
        }
    }

    private void finishTask(AgentRunTask task, AgentRuntimeStatus fallback) {
        AgentRuntimeStatus desired = task.beginFinishing(fallback);
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

    private void handleExecutionException(AgentRunTask task, Exception ex) {
        LOG.warn("Agent任务执行异常，runId={}，异常类型={}", task.runId(), ex.getClass().getSimpleName());
        Thread.interrupted();
        if (ex instanceof AgentOutputException) {
            task.dispose();
        }
        if (task.needsPersistence()) {
            finishTask(task, AgentRuntimeStatus.FAILED);
        }
    }

    private void cleanupTask(AgentRunTask task, ScheduledFuture<?> maintenance) {
        task.markClosed();
        if (maintenance != null) {
            maintenance.cancel(false);
        }
        activeTasks.remove(task.runId());
        capacity.release();
        task.output().close();
    }

    /** 只由同一 Run 的串行维护回调读写；进度保存不等同于官方图 Checkpoint。 */
    private static final class MaintenanceTimes {
        private long lastHeartbeatNanos = System.nanoTime();
        private long lastProgressSaveNanos = lastHeartbeatNanos;
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
        for (AgentRunTask task : activeTasks.values()) {
            task.dispose();
        }
        workers.shutdown();
        try {
            workers.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        workers.shutdownNow();
        clock.shutdownNow();
    }
}
