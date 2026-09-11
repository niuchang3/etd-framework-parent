package org.etd.framework.starter.event.client.core.consumer;

import org.etd.framework.starter.event.client.core.model.EventMessage;

import java.util.List;

/**
 * Kafka 消费入口适配器，为后续订阅监听器提供单条和批量消息调用骨架。
 */
public class KafkaEventConsumerAdapter {

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
        EventMessage message = eventMessageCodec.decode(messageJson);
        eventConsumerInvoker.invoke(message, handler);
    }

    /**
     * 解码后将完整批次交给批量处理器，处理器通过每条 EventMessage 读取独立上下文。
     */
    public void consumeBatch(List<String> messageJsonList, BatchEventMessageHandler handler) throws Exception {
        List<EventMessage> messages = eventMessageCodec.decodeBatch(messageJsonList);
        eventConsumerInvoker.invokeBatch(messages, handler);
    }

    /**
     * 批量拉取后逐条消费，每条消息均自动恢复并清理自己的请求上下文。
     */
    public void consumeEach(List<String> messageJsonList, EventMessageHandler handler) throws Exception {
        List<EventMessage> messages = eventMessageCodec.decodeBatch(messageJsonList);
        eventConsumerInvoker.invokeEach(messages, handler);
    }
}
