package org.etd.framework.starter.event.server.forward;

import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.event.core.sender.EventMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.task.TaskExecutor;

/**
 * 服务端异步事件转发器，透明转发统一消息且不影响消费主流程。
 */
public class EventMessageForwarder {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventMessageForwarder.class);

    private static final String MDC_TRACE_ID = "traceId";

    private final EventMessageSender eventMessageSender;

    private final TaskExecutor taskExecutor;

    public EventMessageForwarder(EventMessageSender eventMessageSender, TaskExecutor taskExecutor) {
        this.eventMessageSender = eventMessageSender;
        this.taskExecutor = taskExecutor;
    }

    /**
     * 异步转发已有事件，所有提交和发送异常只记录日志，不抛回调用线程。
     */
    public void forward(String destination, EventMessage message) {
        try {
            taskExecutor.execute(() -> forwardMessage(destination, message));
        } catch (RuntimeException exception) {
            logForwardFailure(destination, message, exception);
        }
    }

    private void forwardMessage(String destination, EventMessage message) {
        if (destination == null || destination.isBlank() || message == null) {
            logForwardFailure(destination, message,
                    new IllegalArgumentException("事件转发目的地和消息不能为空"));
            return;
        }
        try {
            eventMessageSender.send(destination, message)
                    .whenComplete((result, exception) -> logForwardResult(destination, message, exception));
        } catch (RuntimeException exception) {
            logForwardFailure(destination, message, exception);
        }
    }

    private void logForwardResult(String destination, EventMessage message, Throwable exception) {
        if (exception != null) {
            logForwardFailure(destination, message, exception);
            return;
        }
        setTraceId(message);
        try {
            LOGGER.info("事件转发成功 eventId={}, eventType={}, source={}, destination={}",
                    message.eventId(), message.eventType(), message.source(), destination);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private void logForwardFailure(String destination, EventMessage message, Throwable exception) {
        String eventId = message == null ? null : message.eventId();
        String eventType = message == null ? null : message.eventType();
        setTraceId(message);
        try {
            LOGGER.error("事件转发失败 eventId={}, eventType={}, destination={}",
                    eventId, eventType, destination, exception);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private void setTraceId(EventMessage message) {
        if (message == null) {
            return;
        }
        String traceId = message.context().get(HeaderConstant.TRACE_ID);
        if (traceId != null && !traceId.isBlank()) {
            MDC.put(MDC_TRACE_ID, traceId);
        }
    }
}
