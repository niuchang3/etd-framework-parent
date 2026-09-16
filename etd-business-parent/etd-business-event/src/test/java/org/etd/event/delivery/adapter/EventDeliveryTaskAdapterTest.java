package org.etd.event.delivery.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.event.delivery.entity.EventDeliveryEntity;
import org.etd.event.delivery.converter.EventDeliveryTaskConverter;
import org.etd.event.delivery.mapper.EventDeliveryMapper;
import org.etd.event.eventbus.adapter.EventDeliveryTaskAdapter;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;
import org.etd.framework.starter.event.server.eventbus.model.EventSubscriptionTarget;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 入口事件投递任务登记测试。
 */
class EventDeliveryTaskAdapterTest {

    private final EventDeliveryMapper deliveryMapper = mock(EventDeliveryMapper.class);
    private final EventDeliveryTaskAdapter taskRegistrar =
            new EventDeliveryTaskAdapter(deliveryMapper, new EventDeliveryTaskConverter());

    @Test
    void shouldCreateOneDeliveryTaskForEachSubscription() {
        AtomicLong generatedId = new AtomicLong(100L);
        when(deliveryMapper.insert(any(EventDeliveryEntity.class)))
                .thenAnswer(invocation -> {
                    EventDeliveryEntity delivery = invocation.getArgument(0);
                    delivery.setId(generatedId.getAndIncrement());
                    return 1;
                });

        List<EventDeliveryTask> taskList = taskRegistrar.createDeliveryTaskList(
                createMessage(), 10L, List.of(
                        new EventSubscriptionTarget(20L, "order-created"),
                        new EventSubscriptionTarget(21L, "finance-order-created")));

        assertEquals(2, taskList.size());
        assertEquals(100L, taskList.getFirst().deliveryId());
        assertEquals(20L, taskList.getFirst().subscriptionId());
        assertEquals("order-created", taskList.getFirst().targetTopic());
        ArgumentCaptor<EventDeliveryEntity> captor =
                ArgumentCaptor.forClass(EventDeliveryEntity.class);
        verify(deliveryMapper, times(2)).insert(captor.capture());
        assertEquals("event-100", captor.getAllValues().getFirst().getEventId());
        assertEquals(10L, captor.getAllValues().getFirst().getEventMessageId());
    }

    private EventMessage createMessage() {
        return new EventMessage(
                "event-100",
                "order.created",
                EventMessage.INITIAL_VERSION,
                Instant.parse("2026-09-16T08:00:00Z"),
                "order-service",
                "order-100",
                Map.of(),
                new ObjectMapper().createObjectNode().put("orderId", 100L));
    }

}
