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
import org.etd.framework.starter.event.server.persistence.EventMessagePersistence;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 事件入口持久化用例，将原始消息及其订阅投递任务作为一个事务保存。
 */
@Service
public class EventIngestionBizService implements EventMessagePersistence {

    private final EventTypeService eventTypeService;
    private final EventSubscriptionService subscriptionService;
    private final EventMessageService messageService;
    private final EventDeliveryService deliveryService;
    private final ObjectMapper objectMapper;

    public EventIngestionBizService(EventTypeService eventTypeService,
                                    EventSubscriptionService subscriptionService,
                                    EventMessageService messageService,
                                    EventDeliveryService deliveryService,
                                    ObjectMapper objectMapper) {
        this.eventTypeService = eventTypeService;
        this.subscriptionService = subscriptionService;
        this.messageService = messageService;
        this.deliveryService = deliveryService;
        this.objectMapper = objectMapper;
    }

    /**
     * 幂等保存入口事件，并为接收时处于启用状态的每个订阅创建投递任务。
     *
     * @param message 统一事件消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void persist(EventMessage message) {
        EventMessageEntity persisted = messageService.fetchMessageEntityByEventId(message.eventId());
        if (persisted != null) {
            ensureIdempotentReplay(persisted, message);
            return;
        }
        EventTypeEntity eventType = eventTypeService.requireEventTypeByMessage(
                message.eventType(), message.source(), message.eventVersion());
        Long messageId = messageService.createEventMessage(message, eventType.getId());
        List<EventSubscriptionEntity> subscriptions =
                subscriptionService.selectEnabledSubscriptionListByEventTypeId(eventType.getId());
        deliveryService.createDeliveryList(message, messageId, subscriptions);
    }

    private void ensureIdempotentReplay(EventMessageEntity persisted, EventMessage message) {
        EventTypeEntity eventType = eventTypeService.requireEntity(persisted.getEventTypeId());
        boolean sameMessage = eventType.getEventType().equals(message.eventType())
                && persisted.getEventVersion() == message.eventVersion()
                && persisted.getSourceApplication().equals(message.source())
                && persisted.getOccurredAt().equals(message.occurredAt())
                && Objects.equals(persisted.getPartitionKey(), message.partitionKey())
                && persisted.getEventContext().equals(objectMapper.valueToTree(message.context()))
                && persisted.getEventPayload().equals(message.payload());
        if (!sameMessage) {
            throw new ApiRuntimeException("事件 ID 已被不同的消息内容使用：" + message.eventId());
        }
    }
}
