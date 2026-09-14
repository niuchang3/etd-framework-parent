package org.etd.framework.starter.event.client.core.publisher;

import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.event.core.sender.EventMessageSender;
import org.etd.framework.starter.event.client.config.EventClientProperties;
import org.etd.framework.starter.event.client.core.message.EventMessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.task.TaskExecutor;

/**
 * 默认异步事件发布器，负责创建新事件并提交到底层发送器。
 */
public class DefaultEventPublisher implements EventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEventPublisher.class);

    private static final String MDC_TRACE_ID = "traceId";

    private final EventMessageFactory eventMessageFactory;

    private final EventMessageSender eventMessageSender;

    private final EventClientProperties properties;

    private final TaskExecutor taskExecutor;

    public DefaultEventPublisher(EventMessageFactory eventMessageFactory,
                                 EventMessageSender eventMessageSender,
                                 EventClientProperties properties,
                                 TaskExecutor taskExecutor) {
        this.eventMessageFactory = eventMessageFactory;
        this.eventMessageSender = eventMessageSender;
        this.properties = properties;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void publish(String eventType, Object payload) {
        publish(eventType, EventMessage.INITIAL_VERSION, null, payload);
    }

    @Override
    public void publish(String eventType, String partitionKey, Object payload) {
        publish(eventType, EventMessage.INITIAL_VERSION, partitionKey, payload);
    }

    /**
     * 提交异步构建和发送任务，任何失败均记录日志且不会抛回业务线程。
     */
    @Override
    public void publish(String eventType, int eventVersion, String partitionKey, Object payload) {
        try {
            taskExecutor.execute(() -> createAndSendMessage(eventType, eventVersion, partitionKey, payload));
        } catch (RuntimeException exception) {
            LOGGER.error("事件异步提交失败 eventType={}, destination={}",
                    eventType, properties.getTopic(), exception);
        }
    }

    private void createAndSendMessage(String eventType, int eventVersion, String partitionKey, Object payload) {
        try {
            EventMessage message = eventMessageFactory.create(eventType, eventVersion, partitionKey, payload);
            eventMessageSender.send(properties.getTopic(), message)
                    .whenComplete((result, exception) -> logSendResultWithContext(message, exception));
        } catch (RuntimeException exception) {
            LOGGER.error("事件构建或发送失败 eventType={}, destination={}",
                    eventType, properties.getTopic(), exception);
        }
    }

    private void logSendResultWithContext(EventMessage message, Throwable exception) {
        setTraceId(message);
        try {
            if (exception == null) {
                LOGGER.info("事件发送成功 eventId={}, eventType={}, source={}, destination={}",
                        message.eventId(), message.eventType(), message.source(), properties.getTopic());
            } else {
                LOGGER.error("事件发送失败 eventId={}, eventType={}, source={}, destination={}",
                        message.eventId(), message.eventType(), message.source(), properties.getTopic(), exception);
            }
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private void setTraceId(EventMessage message) {
        String traceId = message.context().get(HeaderConstant.TRACE_ID);
        if (traceId != null && !traceId.isBlank()) {
            MDC.put(MDC_TRACE_ID, traceId);
        }
    }
}
