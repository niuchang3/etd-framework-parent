package org.etd.framework.starter.ai.runtime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.etd.framework.starter.ai.adapter.AgentRunEventListener;
import org.etd.framework.starter.ai.runtime.model.AgentRunProgress;
import org.etd.framework.starter.ai.runtime.model.AgentRunRequest;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEvent;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEventType;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeResult;
import org.springframework.ai.document.Document;

/** 本轮正文、引用与协议输出；网络发送串行化，但不占用任务生命周期锁。 */
final class AgentRunOutput {
    private final String runId;
    private final AgentRunEventListener listener;
    private final Map<String, Object> attributes;
    private final AgentTokenUsage usage;
    private final StringBuilder content = new StringBuilder();
    private List<Document> references = List.of();
    /** 仅保护事件顺序；正文快照使用对象锁，慢网络不能阻塞部分正文保存。 */
    private final Object eventMonitor = new Object();
    private long sequence;
    private final AtomicBoolean disconnected = new AtomicBoolean();
    private volatile boolean executionEnded;

    AgentRunOutput(AgentRunRequest request, AgentTokenUsage usage) {
        this.runId = request.runId();
        this.listener = request.eventListener();
        this.attributes = request.attributes();
        this.usage = usage;
    }

    /** 输出结束标志不等待网络锁，收尾与取消均可独立推进。 */
    void endExecution() {
        executionEnded = true;
    }

    /** 连接断开仅影响输出；任务是否停止由生命周期控制决定。 */
    void disconnect() {
        disconnected.set(true);
    }

    boolean isDisconnected() {
        return disconnected.get();
    }

    synchronized void append(String text, int maximum) {
        if (content.length() + text.length() > maximum) {
            throw new IllegalStateException("模型输出超过本轮字符限制");
        }
        content.append(text);
    }

    synchronized boolean hasContent() {
        return !content.isEmpty();
    }

    /** 持久化使用完整正文；网络事件只携带增量，不重复发送累积正文。 */
    synchronized AgentRunProgress progress() {
        return snapshot(content.toString());
    }

    private synchronized AgentRunProgress eventProgress() {
        return snapshot("");
    }

    private AgentRunProgress snapshot(String text) {
        AgentTokenUsage.Snapshot tokens = usage.snapshot();
        return new AgentRunProgress(runId, text, references, tokens.inputTokens(), tokens.outputTokens());
    }

    /** 空引用也发送事件，保持现有协议的引用初始化语义。 */
    void recordReferences(List<Document> documents) {
        synchronized (eventMonitor) {
            if (documents == null || executionEnded || disconnected.get()) {
                return;
            }
            synchronized (this) {
                references = List.copyOf(documents);
            }
            AgentRunProgress progress = eventProgress();
            sendEvent(AgentRuntimeEventType.REFERENCES, progress, null, null);
        }
    }

    /** 开始、正文增量与心跳共享事件序号及串行发送顺序。 */
    void emit(AgentRuntimeEventType type, String text) {
        synchronized (eventMonitor) {
            if (executionEnded || disconnected.get()) {
                return;
            }
            AgentRunProgress progress = eventProgress();
            sendEvent(type, progress, text, null);
        }
    }

    /** 仅在数据库返回实际终态后发送；断开连接不影响数据库提交。 */
    void emitTerminal(AgentRuntimeResult result) {
        synchronized (eventMonitor) {
            if (!disconnected.get()) {
                sendEvent(AgentRuntimeEventType.TERMINAL, result.progress(), null, result);
            }
        }
    }

    private void sendEvent(AgentRuntimeEventType type, AgentRunProgress progress, String text,
            AgentRuntimeResult result) {
        AgentRuntimeEvent event = new AgentRuntimeEvent(type, ++sequence, progress, text, result, attributes);
        listener.onEvent(event);
    }

    /** 由 Runner 最终清理调用，即使持久化或事件发送失败也释放协议资源。 */
    void close() {
        endExecution();
        listener.close();
    }

}
