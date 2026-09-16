package org.etd.framework.starter.event.server.eventbus;

import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;
import org.etd.framework.starter.event.server.eventbus.model.EventMessageRegistration;
import org.etd.framework.starter.event.server.eventbus.model.EventSubscriptionTarget;
import org.etd.framework.starter.event.server.delivery.port.EventDeliveryTaskRegistrar;
import org.etd.framework.starter.event.server.eventbus.port.EventMessageRegistrar;
import org.etd.framework.starter.event.server.eventbus.port.EventSubscriptionResolver;
import org.etd.framework.starter.event.server.eventbus.port.EventTypeResolver;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 事件总线订阅模板，统一编排类型解析、消息登记、订阅解析和投递任务登记。
 */
public class EventBusSubscriberTemplate {

    private final EventTypeResolver eventTypeResolver;
    private final EventMessageRegistrar messageRegistrar;
    private final EventSubscriptionResolver subscriptionResolver;
    private final EventDeliveryTaskRegistrar deliveryTaskRegistrar;

    public EventBusSubscriberTemplate(EventTypeResolver eventTypeResolver,
                                      EventMessageRegistrar messageRegistrar,
                                      EventSubscriptionResolver subscriptionResolver,
                                      EventDeliveryTaskRegistrar deliveryTaskRegistrar) {
        this.eventTypeResolver = eventTypeResolver;
        this.messageRegistrar = messageRegistrar;
        this.subscriptionResolver = subscriptionResolver;
        this.deliveryTaskRegistrar = deliveryTaskRegistrar;
    }

    /**
     * 在同一事务中登记入口消息及其可靠投递任务。
     *
     * @param message 统一事件消息
     * @return 需要发布到内部投递 Topic 的任务
     */
    @Transactional(rollbackFor = Exception.class)
    public EventBusSubscriberResult receiveEvent(EventMessage message) {
        Long eventTypeId = eventTypeResolver.resolveEventTypeId(message);
        EventMessageRegistration registration =
                messageRegistrar.registerEventMessage(message, eventTypeId);
        List<EventDeliveryTask> taskList = resolveDeliveryTaskList(
                message, eventTypeId, registration);
        return new EventBusSubscriberResult(taskList);
    }

    private List<EventDeliveryTask> resolveDeliveryTaskList(
            EventMessage message,
            Long eventTypeId,
            EventMessageRegistration registration) {
        if (!registration.created()) {
            return deliveryTaskRegistrar.selectDeliveryTaskList(
                    message.eventId(), registration.messageId(), message);
        }
        List<EventSubscriptionTarget> targetList =
                subscriptionResolver.resolveSubscriptionTargetList(eventTypeId);
        return deliveryTaskRegistrar.createDeliveryTaskList(
                message, registration.messageId(), targetList);
    }
}
