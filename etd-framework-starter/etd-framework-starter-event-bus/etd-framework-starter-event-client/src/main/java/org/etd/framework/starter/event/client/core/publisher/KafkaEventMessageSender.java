package org.etd.framework.starter.event.client.core.publisher;

import org.etd.framework.event.core.codec.EventMessageCodec;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.event.core.sender.EventMessageSender;
import org.etd.framework.event.core.sender.EventSendResult;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;

/**
 * 基于 Kafka 的统一事件底层发送器。
 */
public class KafkaEventMessageSender implements EventMessageSender {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    private final EventMessageCodec eventMessageCodec;

    public KafkaEventMessageSender(KafkaTemplate<Object, Object> kafkaTemplate,
                                   EventMessageCodec eventMessageCodec) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventMessageCodec = eventMessageCodec;
    }

    /**
     * 序列化并发送完整事件，转发场景不会重新生成事件 ID 或请求上下文。
     */
    @Override
    public CompletableFuture<EventSendResult> send(String destination, EventMessage message) {
        Assert.hasText(destination, "事件发送目的地不能为空");
        Assert.notNull(message, "事件消息不能为空");
        String messageJson = eventMessageCodec.encode(message);
        String kafkaKey = resolveKafkaKey(message);
        return kafkaTemplate.send(destination, kafkaKey, messageJson)
                .thenApply(result -> new EventSendResult(message.eventId(), destination));
    }

    private String resolveKafkaKey(EventMessage message) {
        return StringUtils.hasText(message.partitionKey()) ? message.partitionKey() : message.eventId();
    }
}
