package org.etd.framework.starter.event.client.core.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.starter.event.client.config.EventBusProperties;
import org.etd.framework.starter.event.client.core.message.EventMessageFactory;
import org.etd.framework.starter.event.client.core.model.EventMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;

/**
 * 基于 Kafka 的事件发布器。
 */
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    private final ObjectMapper objectMapper;

    private final EventMessageFactory eventMessageFactory;

    private final EventBusProperties properties;

    public KafkaEventPublisher(KafkaTemplate<Object, Object> kafkaTemplate,
                               ObjectMapper objectMapper,
                               EventMessageFactory eventMessageFactory,
                               EventBusProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.eventMessageFactory = eventMessageFactory;
        this.properties = properties;
    }

    @Override
    public CompletableFuture<String> publish(String eventType, Object payload) {
        return publish(eventType, EventMessage.INITIAL_VERSION, null, payload);
    }

    @Override
    public CompletableFuture<String> publish(String eventType, String partitionKey, Object payload) {
        return publish(eventType, EventMessage.INITIAL_VERSION, partitionKey, payload);
    }

    @Override
    public CompletableFuture<String> publish(String eventType, int eventVersion,
                                             String partitionKey, Object payload) {
        EventMessage message = eventMessageFactory.create(eventType, eventVersion, partitionKey, payload);
        String messageJson = serializeMessage(message);
        String kafkaKey = StringUtils.hasText(partitionKey) ? partitionKey : message.eventId();
        return kafkaTemplate.send(properties.getTopic(), kafkaKey, messageJson)
                .thenApply(result -> message.eventId());
    }

    private String serializeMessage(EventMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("事件消息无法序列化为 JSON", exception);
        }
    }
}
