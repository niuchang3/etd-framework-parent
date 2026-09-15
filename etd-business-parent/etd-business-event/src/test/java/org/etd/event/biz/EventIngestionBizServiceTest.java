package org.etd.event.biz;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.event.delivery.service.EventDeliveryService;
import org.etd.event.message.entity.EventMessageEntity;
import org.etd.event.message.service.EventMessageService;
import org.etd.event.subscription.entity.EventSubscriptionEntity;
import org.etd.event.subscription.service.EventSubscriptionService;
import org.etd.event.type.entity.EventTypeEntity;
import org.etd.event.type.service.EventTypeService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.event.core.model.EventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 事件入口消息与投递任务事务编排测试。
 */
class EventIngestionBizServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EventTypeService eventTypeService;
    private EventSubscriptionService subscriptionService;
    private EventMessageService messageService;
    private EventDeliveryService deliveryService;
    private EventIngestionBizService ingestionService;

    @BeforeEach
    void setUp() {
        eventTypeService = mock(EventTypeService.class);
        subscriptionService = mock(EventSubscriptionService.class);
        messageService = mock(EventMessageService.class);
        deliveryService = mock(EventDeliveryService.class);
        ingestionService = new EventIngestionBizService(eventTypeService, subscriptionService,
                messageService, deliveryService, objectMapper);
    }

    @Test
    void shouldPersistMessageAndCreateDeliveryTasks() {
        EventMessage message = createMessage();
        EventTypeEntity eventType = new EventTypeEntity();
        eventType.setId(10L);
        List<EventSubscriptionEntity> subscriptions = List.of(createSubscription(20L));
        when(eventTypeService.requireEventTypeByMessage(
                "upms.user.created", "upms", 1)).thenReturn(eventType);
        when(messageService.createEventMessage(message, 10L)).thenReturn(30L);
        when(subscriptionService.selectEnabledSubscriptionListByEventTypeId(10L)).thenReturn(subscriptions);

        ingestionService.persist(message);

        verify(messageService).createEventMessage(message, 10L);
        verify(deliveryService).createDeliveryList(message, 30L, subscriptions);
    }

    @Test
    void shouldIgnoreIdempotentKafkaRedelivery() {
        EventMessage message = createMessage();
        EventMessageEntity persisted = createPersistedMessage(message);
        EventTypeEntity eventType = new EventTypeEntity();
        eventType.setEventType(message.eventType());
        when(messageService.fetchMessageEntityByEventId(message.eventId())).thenReturn(persisted);
        when(eventTypeService.requireEntity(10L)).thenReturn(eventType);

        ingestionService.persist(message);

        verify(messageService, never()).createEventMessage(message, 10L);
        verify(deliveryService, never()).createDeliveryList(any(), any(), anyList());
    }

    @Test
    void shouldRejectDifferentContentUsingExistingEventId() {
        EventMessage message = createMessage();
        EventMessageEntity persisted = createPersistedMessage(message);
        persisted.setSourceApplication("order");
        EventTypeEntity eventType = new EventTypeEntity();
        eventType.setEventType(message.eventType());
        when(messageService.fetchMessageEntityByEventId(message.eventId())).thenReturn(persisted);
        when(eventTypeService.requireEntity(10L)).thenReturn(eventType);

        assertThatThrownBy(() -> ingestionService.persist(message))
                .isInstanceOf(ApiRuntimeException.class)
                .hasMessageContaining("事件 ID 已被不同的消息内容使用");
    }

    private EventMessage createMessage() {
        return new EventMessage("event-1", "upms.user.created", 1,
                Instant.parse("2026-09-15T01:00:00Z"), "upms", "user-1",
                Map.of("traceId", "trace-1"), objectMapper.valueToTree(Map.of("userId", 1L)));
    }

    private EventMessageEntity createPersistedMessage(EventMessage message) {
        EventMessageEntity entity = new EventMessageEntity();
        entity.setEventId(message.eventId());
        entity.setEventTypeId(10L);
        entity.setEventVersion(message.eventVersion());
        entity.setOccurredAt(message.occurredAt());
        entity.setSourceApplication(message.source());
        entity.setPartitionKey(message.partitionKey());
        entity.setEventContext(objectMapper.valueToTree(message.context()));
        entity.setEventPayload(message.payload());
        return entity;
    }

    private EventSubscriptionEntity createSubscription(Long id) {
        EventSubscriptionEntity entity = new EventSubscriptionEntity();
        entity.setId(id);
        entity.setTargetTopic("etd.event.sub.upms");
        return entity;
    }
}
