package org.etd.framework.starter.event.server.eventbus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;
import org.etd.framework.starter.event.server.eventbus.model.EventMessageRegistration;
import org.etd.framework.starter.event.server.eventbus.model.EventSubscriptionTarget;
import org.etd.framework.starter.event.server.delivery.port.EventDeliveryTaskRegistrar;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 事件总线订阅模板固定流程和幂等分支测试。
 */
class EventBusSubscriberTemplateTest {

    private final EventMessage message = createMessage();
    private final EventDeliveryTaskRegistrar taskRegistrar = mock(EventDeliveryTaskRegistrar.class);

    @Test
    void shouldResolveSubscriptionsAndCreateTasksForNewMessage() {
        List<EventSubscriptionTarget> targetList =
                List.of(new EventSubscriptionTarget(20L, "event.sub.upms"));
        EventDeliveryTask task = createTask();
        EventBusSubscriberTemplate template = new EventBusSubscriberTemplate(
                ignored -> 10L,
                (ignored, eventTypeId) -> new EventMessageRegistration(30L, true),
                eventTypeId -> targetList,
                taskRegistrar);
        when(taskRegistrar.createDeliveryTaskList(message, 30L, targetList))
                .thenReturn(List.of(task));

        EventBusSubscriberResult result = template.receiveEvent(message);

        assertThat(result.deliveryTaskList()).containsExactly(task);
        verify(taskRegistrar).createDeliveryTaskList(message, 30L, targetList);
        verify(taskRegistrar, never()).selectDeliveryTaskList("event-1", 30L, message);
    }

    @Test
    void shouldLoadExistingTasksForRepeatedMessage() {
        EventDeliveryTask task = createTask();
        EventBusSubscriberTemplate template = new EventBusSubscriberTemplate(
                ignored -> 10L,
                (ignored, eventTypeId) -> new EventMessageRegistration(30L, false),
                eventTypeId -> {
                    throw new AssertionError("重复消息不应重新解析订阅");
                },
                taskRegistrar);
        when(taskRegistrar.selectDeliveryTaskList("event-1", 30L, message))
                .thenReturn(List.of(task));

        EventBusSubscriberResult result = template.receiveEvent(message);

        assertThat(result.deliveryTaskList()).containsExactly(task);
        verify(taskRegistrar).selectDeliveryTaskList("event-1", 30L, message);
    }

    private EventMessage createMessage() {
        return new EventMessage(
                "event-1", "upms.user.created", 1,
                Instant.parse("2026-09-15T01:00:00Z"), "upms", "user-1", Map.of(),
                new ObjectMapper().valueToTree(Map.of("userId", 1L)));
    }

    private EventDeliveryTask createTask() {
        return new EventDeliveryTask(40L, 30L, 20L,
                "event-1", "event.sub.upms", message);
    }
}
