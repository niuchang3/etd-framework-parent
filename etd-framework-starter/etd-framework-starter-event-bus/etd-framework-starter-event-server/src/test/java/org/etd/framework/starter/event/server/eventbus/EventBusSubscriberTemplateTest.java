package org.etd.framework.starter.event.server.eventbus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;
import org.etd.framework.starter.event.server.delivery.port.EventDeliveryTaskRegistrar;
import org.etd.framework.starter.event.server.eventbus.model.EventMessageRegistration;
import org.etd.framework.starter.event.server.eventbus.model.EventMessageStatus;
import org.etd.framework.starter.event.server.eventbus.model.EventSubscriptionTarget;
import org.etd.framework.starter.event.server.eventbus.model.EventTypeDefinition;
import org.etd.framework.starter.event.server.eventbus.port.EventMessageRegistrar;
import org.etd.framework.starter.event.server.eventbus.port.EventSubscriptionResolver;
import org.etd.framework.starter.event.server.eventbus.port.EventTypeResolver;
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
 * 事件总线订阅模板固定流程、业务校验和幂等分支测试。
 */
class EventBusSubscriberTemplateTest {

    private final EventMessage message = createMessage();
    private final EventTypeResolver typeResolver = mock(EventTypeResolver.class);
    private final EventMessageRegistrar messageRegistrar = mock(EventMessageRegistrar.class);
    private final EventSubscriptionResolver subscriptionResolver =
            mock(EventSubscriptionResolver.class);
    private final EventDeliveryTaskRegistrar taskRegistrar = mock(EventDeliveryTaskRegistrar.class);
    private final EventBusSubscriberTemplate template = new EventBusSubscriberTemplate(
            typeResolver, messageRegistrar, subscriptionResolver, taskRegistrar);

    @Test
    void shouldCreateNormalMessageAndDeliveryTasks() {
        EventTypeDefinition definition = createTypeDefinition(true);
        List<EventSubscriptionTarget> targetList =
                List.of(new EventSubscriptionTarget(20L, "event.sub.upms"));
        EventDeliveryTask task = createTask();
        when(typeResolver.selectEventType(message.eventType())).thenReturn(definition);
        when(messageRegistrar.createEventMessage(
                message, 10L, EventMessageStatus.NORMAL, null)).thenReturn(30L);
        when(subscriptionResolver.resolveSubscriptionTargetList(10L)).thenReturn(targetList);
        when(taskRegistrar.createDeliveryTaskList(message, 30L, targetList))
                .thenReturn(List.of(task));

        EventBusSubscriberResult result = template.receiveEvent(message);

        assertThat(result.deliveryTaskList()).containsExactly(task);
        verify(messageRegistrar).createEventMessage(
                message, 10L, EventMessageStatus.NORMAL, null);
    }

    @Test
    void shouldStoreErrorMessageWithoutCreatingTasksWhenTypeDoesNotExist() {
        when(messageRegistrar.createEventMessage(
                message, null, EventMessageStatus.ERROR,
                "事件类型不存在：upms.user.created")).thenReturn(30L);

        EventBusSubscriberResult result = template.receiveEvent(message);

        assertThat(result.deliveryTaskList()).isEmpty();
        verify(messageRegistrar).createEventMessage(
                message, null, EventMessageStatus.ERROR,
                "事件类型不存在：upms.user.created");
        verify(subscriptionResolver, never()).resolveSubscriptionTargetList(10L);
    }

    @Test
    void shouldStoreErrorMessageWhenSourceDoesNotMatchDefinition() {
        EventTypeDefinition definition = new EventTypeDefinition(
                10L, message.eventType(), "order", 1, true);
        when(typeResolver.selectEventType(message.eventType())).thenReturn(definition);
        when(messageRegistrar.createEventMessage(
                message, 10L, EventMessageStatus.ERROR,
                "事件来源应用与事件类型定义不一致。")).thenReturn(30L);

        EventBusSubscriberResult result = template.receiveEvent(message);

        assertThat(result.deliveryTaskList()).isEmpty();
        verify(messageRegistrar).createEventMessage(
                message, 10L, EventMessageStatus.ERROR,
                "事件来源应用与事件类型定义不一致。");
        verify(taskRegistrar, never()).createDeliveryTaskList(
                message, 30L, List.of());
    }

    @Test
    void shouldStoreErrorMessageWithoutCreatingTasksWhenSubscriptionDoesNotExist() {
        EventTypeDefinition definition = createTypeDefinition(true);
        when(typeResolver.selectEventType(message.eventType())).thenReturn(definition);
        when(subscriptionResolver.resolveSubscriptionTargetList(10L)).thenReturn(List.of());
        when(messageRegistrar.createEventMessage(
                message, 10L, EventMessageStatus.ERROR,
                "事件类型未配置启用的订阅关系：upms.user.created")).thenReturn(30L);

        EventBusSubscriberResult result = template.receiveEvent(message);

        assertThat(result.deliveryTaskList()).isEmpty();
        verify(messageRegistrar).createEventMessage(
                message, 10L, EventMessageStatus.ERROR,
                "事件类型未配置启用的订阅关系：upms.user.created");
        verify(taskRegistrar, never()).createDeliveryTaskList(
                message, 30L, List.of());
    }

    @Test
    void shouldLoadExistingTasksForRepeatedNormalMessage() {
        EventDeliveryTask task = createTask();
        EventMessageRegistration registration = new EventMessageRegistration(
                30L, message, 10L, EventMessageStatus.NORMAL, null);
        when(messageRegistrar.selectEventMessage(message.eventId())).thenReturn(registration);
        when(taskRegistrar.selectDeliveryTaskList("event-1", 30L, message))
                .thenReturn(List.of(task));

        EventBusSubscriberResult result = template.receiveEvent(message);

        assertThat(result.deliveryTaskList()).containsExactly(task);
        verify(taskRegistrar).selectDeliveryTaskList("event-1", 30L, message);
        verify(typeResolver, never()).selectEventType(message.eventType());
    }

    @Test
    void shouldIgnoreRepeatedErrorMessage() {
        EventMessageRegistration registration = new EventMessageRegistration(
                30L, message, null, EventMessageStatus.ERROR, "事件类型不存在");
        when(messageRegistrar.selectEventMessage(message.eventId())).thenReturn(registration);

        EventBusSubscriberResult result = template.receiveEvent(message);

        assertThat(result.deliveryTaskList()).isEmpty();
        verify(typeResolver, never()).selectEventType(message.eventType());
        verify(taskRegistrar, never()).selectDeliveryTaskList("event-1", 30L, message);
    }

    private EventTypeDefinition createTypeDefinition(boolean enabled) {
        return new EventTypeDefinition(
                10L, message.eventType(), message.source(), message.eventVersion(), enabled);
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
