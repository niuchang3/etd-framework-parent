package org.etd.framework.starter.event.server.eventbus;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 事件总线订阅模板，统一编排类型解析、消息登记、订阅解析和投递任务登记。
 */
public class EventBusSubscriberTemplate {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBusSubscriberTemplate.class);

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
        EventMessageRegistration registration =
                messageRegistrar.selectEventMessage(message.eventId());
        if (registration != null) {
            return resolveRegisteredMessage(message, registration);
        }
        EventMessageDecision decision = resolveMessageDecision(message);
        Long messageId = messageRegistrar.createEventMessage(
                message, decision.eventTypeId(), decision.status(), decision.failureReason());
        List<EventDeliveryTask> taskList = createDeliveryTaskList(message, messageId, decision);
        return new EventBusSubscriberResult(taskList);
    }

    private EventBusSubscriberResult resolveRegisteredMessage(
            EventMessage message, EventMessageRegistration registration) {
        if (!registration.message().equals(message)) {
            LOGGER.error("事件 ID 已被不同的消息内容使用 eventId={}", message.eventId());
            return new EventBusSubscriberResult(List.of());
        }
        if (registration.status() == EventMessageStatus.ERROR) {
            return new EventBusSubscriberResult(List.of());
        }
        if (registration.eventTypeId() == null) {
            LOGGER.error("正常入口消息缺少事件类型主键 eventId={}, messageId={}",
                    message.eventId(), registration.messageId());
            return new EventBusSubscriberResult(List.of());
        }
        List<EventDeliveryTask> taskList = deliveryTaskRegistrar.selectDeliveryTaskList(
                message.eventId(), registration.messageId(), message);
        return new EventBusSubscriberResult(taskList);
    }

    private EventMessageDecision resolveMessageDecision(EventMessage message) {
        EventTypeDefinition definition = eventTypeResolver.selectEventType(message.eventType());
        if (definition == null) {
            return EventMessageDecision.error(null, "事件类型不存在：" + message.eventType());
        }
        if (!definition.enabled()) {
            return EventMessageDecision.error(
                    definition.eventTypeId(), "事件类型未启用：" + message.eventType());
        }
        if (!definition.sourceApplication().equals(message.source())) {
            return EventMessageDecision.error(
                    definition.eventTypeId(), "事件来源应用与事件类型定义不一致。");
        }
        if (message.eventVersion() > definition.latestVersion()) {
            return EventMessageDecision.error(
                    definition.eventTypeId(), "事件协议版本高于事件中心登记的最新版本。");
        }
        return EventMessageDecision.normal(definition.eventTypeId());
    }

    private List<EventDeliveryTask> createDeliveryTaskList(
            EventMessage message, Long messageId, EventMessageDecision decision) {
        if (decision.status() == EventMessageStatus.ERROR) {
            return List.of();
        }
        List<EventSubscriptionTarget> targetList =
                subscriptionResolver.resolveSubscriptionTargetList(decision.eventTypeId());
        return deliveryTaskRegistrar.createDeliveryTaskList(
                message, messageId, targetList);
    }

    /**
     * starter-server 对入口消息作出的最终持久化决定。
     *
     * @param eventTypeId 事件类型主键，仅在类型不存在时为空
     * @param status 消息处理状态
     * @param failureReason 业务校验失败原因，正常消息为空
     */
    private record EventMessageDecision(
            Long eventTypeId, EventMessageStatus status, String failureReason) {

        private static EventMessageDecision normal(Long eventTypeId) {
            return new EventMessageDecision(eventTypeId, EventMessageStatus.NORMAL, null);
        }

        private static EventMessageDecision error(Long eventTypeId, String failureReason) {
            return new EventMessageDecision(
                    eventTypeId, EventMessageStatus.ERROR, failureReason);
        }
    }
}
