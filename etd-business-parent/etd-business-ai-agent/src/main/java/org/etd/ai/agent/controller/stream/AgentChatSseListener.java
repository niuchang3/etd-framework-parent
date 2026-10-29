package org.etd.ai.agent.controller.stream;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.etd.ai.agent.constant.AgentChatPolicy;
import org.etd.ai.agent.converter.AgentChatConverter;
import org.etd.ai.agent.model.AgentChatEvent;
import org.etd.framework.starter.ai.runtime.AgentRunControl;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEvent;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEventType;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;
import org.etd.framework.starter.ai.runtime.spi.AgentOutputException;
import org.etd.framework.starter.ai.runtime.spi.AgentRunEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import org.etd.ai.agent.model.AgentCitation;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeResult;

/** 每次HTTP请求独立的SSE输出插槽实现，只负责协议映射，不执行模型、线程调度或数据库操作。 */
public final class AgentChatSseListener implements AgentRunEventListener {
    private final SseEmitter emitter;
    private final AtomicBoolean finished = new AtomicBoolean();
    private final AtomicBoolean disconnected = new AtomicBoolean();
    private volatile AgentRunControl handle;

    /** @param emitter 本次HTTP输出通道 */
    public AgentChatSseListener(SseEmitter emitter) {
        this.emitter = emitter;
        emitter.onCompletion(this::disconnect);
        emitter.onTimeout(this::disconnect);
        emitter.onError(error -> disconnect());
    }

    /** @param handle 框架启动后返回的控制句柄；若先发生断线则立即取消 */
    public void bind(AgentRunControl handle) {
        this.handle = handle;
        if (disconnected.get()) {
            handle.cancel();
        }
    }

    /** @param event 框架运行事件 @throws AgentOutputException 客户端断线 */
    @Override
    public void onEvent(AgentRuntimeEvent event) {
        try {
            if (event.type() == AgentRuntimeEventType.HEARTBEAT) {
                SseEmitter.SseEventBuilder heartbeat = SseEmitter.event().comment("heartbeat");
                emitter.send(heartbeat);
                return;
            }
            AgentChatEvent data = toEvent(event);
            String name = eventName(event);
            if (event.type() == AgentRuntimeEventType.TERMINAL) {
                finished.set(true);
            }
            SseEmitter.SseEventBuilder outgoing = SseEmitter.event().name(name).id(data.getSequence().toString()).data(data);
            // MVC绑定输出通道后，send同步写出完整SSE事件并flush；不另加异步队列或等待终态批量发送。
            // 绑定前的早期事件由Spring在初始化通道时写出，不能提前flush原始response破坏异常响应。
            emitter.send(outgoing);
        } catch (IOException | IllegalStateException exception) {
            disconnect();
            throw new AgentOutputException();
        }
    }

    private AgentChatEvent toEvent(AgentRuntimeEvent input) {
        AgentChatEvent event = new AgentChatEvent();
        event.setRunId(Long.valueOf(input.progress().executionId()));
        event.setSessionId((Long) input.attributes().get(AgentChatPolicy.SESSION_ID));
        event.setMessageId((Long) input.attributes().get(AgentChatPolicy.MESSAGE_ID));
        event.setSequence(input.sequence());
        event.setText(input.text());
        if (input.type() == AgentRuntimeEventType.REFERENCES) {
            List<AgentCitation> citations = AgentChatConverter.INSTANCE.toCitationList(input.progress().references());
            event.setReferences(citations);
        }
        if (input.result() != null) {
            AgentRuntimeResult result = input.result();
            event.setRunStatus(result.status().getCode());
            event.setInputTokens(result.progress().inputTokens());
            event.setOutputTokens(result.progress().outputTokens());
            event.setErrorCode(result.errorCode());
            event.setErrorMessage(result.errorMessage());
        }
        return event;
    }

    private String eventName(AgentRuntimeEvent event) {
        return switch (event.type()) {
            case START -> AgentChatPolicy.START;
            case REFERENCES -> AgentChatPolicy.REFERENCES;
            case DELTA -> AgentChatPolicy.DELTA;
            case TERMINAL -> event.result().status() == AgentRuntimeStatus.FAILED ? AgentChatPolicy.ERROR : AgentChatPolicy.DONE;
            case HEARTBEAT -> throw new IllegalStateException("心跳不使用数据事件");
        };
    }

    private void disconnect() {
        if (!finished.get()) {
            disconnected.set(true);
            AgentRunControl current = handle;
            if (current != null) {
                current.cancel();
            }
        }
    }

    /** 运行终态后释放HTTP通道，不再触发取消。 */
    @Override
    public void close() {
        finished.set(true);
        emitter.complete();
    }
}
