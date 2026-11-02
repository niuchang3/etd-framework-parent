package org.etd.framework.starter.ai.runtime;

import com.alibaba.cloud.ai.graph.agent.tool.CancellationToken;
import com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.etd.framework.starter.ai.runtime.model.AgentRunRequest;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * 单节点任务生命周期与停止协调，不作为数据库终态的权威来源。
 * 生命周期依次为：执行中、收尾中、已保存、已关闭；异常保存失败时允许从收尾阶段重试失败落库。
 * 所有可变生命周期字段均由对象锁保护；输出和用量各自管理，不在状态锁内执行网络或第三方回调。
 */
final class AgentRunTask implements AgentRunControl {
    private static final Logger LOG = LoggerFactory.getLogger(AgentRunTask.class);
    private final AgentRunRequest request;
    private final AgentTokenUsage usage = new AgentTokenUsage();
    private final AgentRunOutput output;
    private final DefaultCancellationToken toolCancellation = new DefaultCancellationToken();
    private final Sinks.One<Boolean> stopSignal = Sinks.one();
    private final long startedNanos = System.nanoTime();
    private final long timeoutNanos;
    private Phase phase = Phase.EXECUTING;
    /** 停止原因与生命周期分开：请求停止后，仍必须保存部分正文及数据库终态。 */
    private AgentRuntimeStatus requestedStop;
    private Thread worker;
    private Thread activeToolThread;

    AgentRunTask(AgentRunRequest request, int timeoutSeconds) {
        this.request = request;
        this.output = new AgentRunOutput(request, usage);
        this.timeoutNanos = TimeUnit.SECONDS.toNanos(timeoutSeconds);
    }

    AgentRunRequest request() {
        return request;
    }

    AgentRunOutput output() {
        return output;
    }

    @Override
    public String runId() {
        return request.runId();
    }

    /** 关闭客户端输出与停止执行分别处理，网络阻塞不能阻止发出取消信号。 */
    @Override
    public void dispose() {
        output.disconnect();
        stop(AgentRuntimeStatus.CANCELLED);
    }

    @Override
    public synchronized boolean isDisposed() {
        return output.isDisconnected() || requestedStop != null || phase == Phase.PERSISTED || phase == Phase.CLOSED;
    }

    synchronized boolean isActive() {
        return phase == Phase.EXECUTING && requestedStop == null;
    }

    @Override
    public synchronized void requireActive() {
        if (!isActive()) {
            throw new IllegalStateException("任务已停止");
        }
    }

    /** 与停止使用同一把锁；取消早于线程启动时，执行入口的活跃检查会直接拒绝执行。 */
    synchronized void registerWorkerThread() {
        worker = Thread.currentThread();
    }

    @Override
    public synchronized void registerToolThread() {
        requireActive();
        activeToolThread = Thread.currentThread();
    }

    @Override
    public synchronized void unregisterToolThread() {
        activeToolThread = null;
    }

    @Override
    public CancellationToken cancellationToken() {
        return toolCancellation;
    }

    /** 模型流订阅此信号；工具同时使用官方取消令牌，不另外实现工具执行循环。 */
    Mono<Boolean> stopSignal() {
        return stopSignal.asMono();
    }

    @Override
    public Duration remainingTime() {
        long remaining = timeoutNanos - (System.nanoTime() - startedNanos);
        return Duration.ofNanos(Math.max(1, remaining));
    }

    boolean hasTimedOut() {
        return System.nanoTime() - startedNanos >= timeoutNanos;
    }

    @Override
    public void fail() {
        stop(AgentRuntimeStatus.FAILED);
    }

    @Override
    public void recordReferences(List<Document> references) {
        if (isActive()) {
            output.recordReferences(references);
        }
    }

    @Override
    public void recordUsage(Long inputTokens, Long outputTokens) {
        usage.recordCumulativeUsage(inputTokens, outputTokens);
    }

    @Override
    public void nextCallRound() {
        usage.beginModelCall();
    }

    /**
     * 原子结束执行阶段并选取待保存结果；数据库仍可通过条件更新返回实际取消终态。
     * 收尾阶段解除线程登记，后续取消不再中断数据库提交；持久化失败仍允许 Runner 进行失败收尾。
     */
    synchronized AgentRuntimeStatus beginFinishing(AgentRuntimeStatus fallback) {
        phase = Phase.FINISHING;
        worker = null;
        activeToolThread = null;
        output.endExecution();
        return requestedStop == null ? fallback : requestedStop;
    }

    /** 只有存储返回成功才标记已保存，避免终态事件发送失败时重复提交结果。 */
    synchronized void markPersisted() {
        phase = Phase.PERSISTED;
    }

    /** 执行中和收尾中仍需落库；已经保存或已经关闭时不能重复进入失败收尾。 */
    synchronized boolean needsPersistence() {
        return phase == Phase.EXECUTING || phase == Phase.FINISHING;
    }

    /** 无论保存是否成功，清理后均不能再登记或中断已经归还的线程。 */
    synchronized void markClosed() {
        phase = Phase.CLOSED;
        worker = null;
        activeToolThread = null;
        output.endExecution();
    }

    /** 首次停止请求生效；锁内协调线程，锁外通知可能执行第三方代码的取消回调。 */
    void stop(AgentRuntimeStatus status) {
        synchronized (this) {
            if (phase != Phase.EXECUTING || requestedStop != null) {
                return;
            }
            requestedStop = status;
            interruptActiveThreads();
        }
        try {
            toolCancellation.cancel();
        } catch (RuntimeException failure) {
            LOG.warn("Agent工具取消回调异常，runId={}，异常类型={}", runId(), failure.getClass().getSimpleName());
        } finally {
            stopSignal.tryEmitValue(true);
        }
    }

    /** 仅在持有生命周期锁时调用，与线程登记、注销及进入收尾阶段互斥。 */
    private void interruptActiveThreads() {
        if (activeToolThread != null && activeToolThread != Thread.currentThread()) {
            activeToolThread.interrupt();
        }
        if (worker != null && worker != Thread.currentThread()) {
            worker.interrupt();
        }
    }

    /** 本地生命周期，不序列化、不替代业务 Run 状态；保存完成与资源关闭明确区分。 */
    private enum Phase {
        /** Agent 执行及工具调用阶段，允许请求停止和中断线程。 */
        EXECUTING,
        /** 执行已经退出，正在保存终态，禁止取消中断数据库提交。 */
        FINISHING,
        /** 数据库已经返回实际终态，仅剩终态事件和资源清理。 */
        PERSISTED,
        /** 本地资源已经进入最终清理，不代表数据库一定保存成功。 */
        CLOSED
    }
}
