package org.etd.framework.starter.ai.runtime;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.etd.framework.starter.ai.runtime.model.AgentRunRequest;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;
import org.etd.framework.starter.ai.runtime.model.AgentRunProgress;
import org.etd.framework.starter.ai.runtime.model.KnowledgeFragment;
import reactor.core.publisher.Sinks;

/** 单节点内存执行句柄，不作为跨节点状态权威，不保存到数据库或缓存。 */
final class AgentRunTask implements AgentRunControl {
    final AgentRunRequest runRequest;
    final Sinks.One<Boolean> cancellation = Sinks.one();
    final AtomicReference<AgentRuntimeStatus> stopStatus = new AtomicReference<>();
    final AtomicBoolean disconnected = new AtomicBoolean();
    final AtomicBoolean finished = new AtomicBoolean();
    final AtomicLong sequence = new AtomicLong();
    final long startedNanos = System.nanoTime();
    long lastHeartbeatNanos = startedNanos;
    long lastCheckpointNanos = startedNanos;
    volatile Thread worker;
    volatile List<KnowledgeFragment> references = List.of();
    volatile Long inputTokens;
    volatile Long outputTokens;
    private final StringBuilder content = new StringBuilder();

    AgentRunTask(AgentRunRequest runRequest) {
        this.runRequest = runRequest;
    }

    @Override
    public String executionId() {
        return runRequest.executionId();
    }

    @Override
    public void cancel() {
        disconnected.set(true);
        stop(AgentRuntimeStatus.CANCELLED);
    }

    AgentRunProgress progress() {
        String text = content();
        return new AgentRunProgress(executionId(), text, references, inputTokens, outputTokens);
    }

    AgentRunProgress eventSnapshot() {
        return new AgentRunProgress(executionId(), "", references, inputTokens, outputTokens);
    }

    synchronized void append(String text, int maximum) {
        if (content.length() + text.length() > maximum) {
            throw new IllegalStateException("模型输出超过本轮字符限制");
        }
        content.append(text);
    }

    synchronized String content() {
        return content.toString();
    }

    void stop(AgentRuntimeStatus status) {
        if (finished.get()) {
            return;
        }
        stopStatus.compareAndSet(null, status);
        cancellation.tryEmitValue(true);
        Thread thread = worker;
        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
        }
    }
}
