package org.etd.framework.starter.event.client.core.publisher;

import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.event.core.sender.EventMessageSender;
import org.etd.framework.starter.event.client.config.EventClientProperties;
import org.etd.framework.starter.event.client.core.message.EventMessageFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 默认事件发布器，负责创建新事件并发送到总线入口。
 */
public class DefaultEventPublisher implements EventPublisher {

    private final EventMessageFactory eventMessageFactory;

    private final EventMessageSender eventMessageSender;

    private final EventClientProperties properties;

    public DefaultEventPublisher(EventMessageFactory eventMessageFactory,
                                 EventMessageSender eventMessageSender,
                                 EventClientProperties properties) {
        this.eventMessageFactory = eventMessageFactory;
        this.eventMessageSender = eventMessageSender;
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
        return eventMessageSender.send(properties.getTopic(), message)
                .thenApply(result -> result.eventId());
    }
}
