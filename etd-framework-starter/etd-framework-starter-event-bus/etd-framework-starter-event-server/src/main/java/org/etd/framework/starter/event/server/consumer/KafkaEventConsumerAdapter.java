package org.etd.framework.starter.event.server.consumer;

import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.event.core.codec.EventMessageCodec;
import org.etd.framework.event.core.model.EventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;

/**
 * Kafka 消费入口适配器，为后续订阅监听器提供单条和批量消息调用骨架。
 */
public class KafkaEventConsumerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventConsumerAdapter.class);

    private static final String MDC_TRACE_ID = "traceId";

    private final EventMessageCodec eventMessageCodec;

    private final EventConsumerInvoker eventConsumerInvoker;

    public KafkaEventConsumerAdapter(EventMessageCodec eventMessageCodec,
                                     EventConsumerInvoker eventConsumerInvoker) {
        this.eventMessageCodec = eventMessageCodec;
        this.eventConsumerInvoker = eventConsumerInvoker;
    }

    /**
     * 解码并消费单条 Kafka 消息。
     */
    public void consume(String messageJson, EventMessageHandler handler) throws Exception {
        EventMessage message = decodeMessage(messageJson);
        logReceivedMessage(message);
        try {
            eventConsumerInvoker.invoke(message, handler);
        } catch (Exception exception) {
            logHandleFailure(message, exception);
            throw exception;
        }
    }

    /**
     * 解码后将完整批次交给批量处理器，处理器通过每条消息读取独立上下文。
     */
    public void consumeBatch(List<String> messageJsonList, BatchEventMessageHandler handler) throws Exception {
        List<EventMessage> messages = decodeMessages(messageJsonList);
        logReceivedMessages(messages);
        try {
            eventConsumerInvoker.invokeBatch(messages, handler);
        } catch (Exception exception) {
            LOGGER.error("批量事件处理失败 count={}", messages.size(), exception);
            throw exception;
        }
    }

    /**
     * 批量拉取后逐条消费，每条消息均自动恢复并清理自己的请求上下文。
     */
    public void consumeEach(List<String> messageJsonList, EventMessageHandler handler) throws Exception {
        List<EventMessage> messages = decodeMessages(messageJsonList);
        logReceivedMessages(messages);
        try {
            eventConsumerInvoker.invokeEach(messages, handler);
        } catch (Exception exception) {
            LOGGER.error("逐条事件处理失败 count={}", messages.size(), exception);
            throw exception;
        }
    }

    private EventMessage decodeMessage(String messageJson) {
        try {
            return eventMessageCodec.decode(messageJson);
        } catch (RuntimeException exception) {
            LOGGER.error("事件消息解码失败", exception);
            throw exception;
        }
    }

    private List<EventMessage> decodeMessages(List<String> messageJsonList) {
        try {
            return eventMessageCodec.decodeBatch(messageJsonList);
        } catch (RuntimeException exception) {
            LOGGER.error("批量事件消息解码失败 count={}", messageJsonList.size(), exception);
            throw exception;
        }
    }

    private void logReceivedMessages(List<EventMessage> messages) {
        for (EventMessage message : messages) {
            logReceivedMessage(message);
        }
    }

    private void logReceivedMessage(EventMessage message) {
        setTraceId(message);
        try {
            LOGGER.info("接收到事件 eventId={}, eventType={}, source={}",
                    message.eventId(), message.eventType(), message.source());
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private void logHandleFailure(EventMessage message, Exception exception) {
        setTraceId(message);
        try {
            LOGGER.error("事件处理失败 eventId={}, eventType={}, source={}",
                    message.eventId(), message.eventType(), message.source(), exception);
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
