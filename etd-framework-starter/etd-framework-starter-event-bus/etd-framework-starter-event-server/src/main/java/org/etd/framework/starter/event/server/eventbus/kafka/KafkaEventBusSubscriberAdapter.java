package org.etd.framework.starter.event.server.eventbus.kafka;

import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.event.core.codec.EventMessageCodec;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.eventbus.EventBusSubscriberProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;

/**
 * Kafka 统一入口 Topic 适配器，负责协议解码、接收日志和入口处理调用。
 */
public class KafkaEventBusSubscriberAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventBusSubscriberAdapter.class);
    private static final String MDC_TRACE_ID = "traceId";

    private final EventMessageCodec eventMessageCodec;
    private final EventBusSubscriberProcessor subscriberProcessor;

    public KafkaEventBusSubscriberAdapter(EventMessageCodec eventMessageCodec,
                                          EventBusSubscriberProcessor subscriberProcessor) {
        this.eventMessageCodec = eventMessageCodec;
        this.subscriberProcessor = subscriberProcessor;
    }

    /**
     * 解码并处理一条 Kafka 入口消息。
     *
     * @param messageJson 统一事件协议 JSON
     * @throws Exception 解码或入口处理失败
     */
    public void consumeEvent(String messageJson) throws Exception {
        EventMessage message = decodeMessage(messageJson);
        logReceivedMessage(message);
        try {
            subscriberProcessor.processEvent(message);
        } catch (Exception exception) {
            logHandleFailure(message, exception);
            throw exception;
        }
    }

    /**
     * 解码批量 Kafka 消息并逐条执行入口处理。
     *
     * @param messageJsonList 统一事件协议 JSON 列表
     * @throws Exception 任意消息解码或入口处理失败
     */
    public void consumeEventList(List<String> messageJsonList) throws Exception {
        List<EventMessage> messageList = decodeMessageList(messageJsonList);
        logReceivedMessageList(messageList);
        try {
            subscriberProcessor.processEventList(messageList);
        } catch (Exception exception) {
            LOGGER.error("入口事件逐条处理失败 count={}", messageList.size(), exception);
            throw exception;
        }
    }

    private EventMessage decodeMessage(String messageJson) {
        try {
            return eventMessageCodec.decode(messageJson);
        } catch (RuntimeException exception) {
            LOGGER.error("入口事件消息解码失败", exception);
            throw exception;
        }
    }

    private List<EventMessage> decodeMessageList(List<String> messageJsonList) {
        try {
            return eventMessageCodec.decodeBatch(messageJsonList);
        } catch (RuntimeException exception) {
            LOGGER.error("批量入口事件消息解码失败 count={}", messageJsonList.size(), exception);
            throw exception;
        }
    }

    private void logReceivedMessageList(List<EventMessage> messageList) {
        for (EventMessage message : messageList) {
            logReceivedMessage(message);
        }
    }

    private void logReceivedMessage(EventMessage message) {
        setTraceId(message);
        try {
            LOGGER.info("接收到入口事件 eventId={}, eventType={}, source={}",
                    message.eventId(), message.eventType(), message.source());
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private void logHandleFailure(EventMessage message, Exception exception) {
        setTraceId(message);
        try {
            LOGGER.error("入口事件处理失败 eventId={}, eventType={}, source={}",
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
