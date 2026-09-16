package org.etd.event.delivery.biz;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.event.delivery.converter.EventDeliveryTaskConverter;
import org.etd.event.delivery.entity.EventDeliveryEntity;
import org.etd.event.delivery.service.EventDeliveryReplayService;
import org.etd.event.delivery.service.EventDeliveryService;
import org.etd.event.message.entity.EventMessageEntity;
import org.etd.event.message.service.EventMessageService;
import org.etd.event.type.entity.EventTypeEntity;
import org.etd.event.type.service.EventTypeService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;
import org.etd.framework.starter.event.server.delivery.producer.EventDeliveryTaskPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 失败投递任务的人工重播业务编排。
 */
@Service
public class EventDeliveryReplayBizService {

    private final EventDeliveryService deliveryService;
    private final EventDeliveryReplayService replayService;
    private final EventMessageService messageService;
    private final EventTypeService eventTypeService;
    private final EventDeliveryTaskConverter taskConverter;
    private final EventDeliveryTaskPublisher taskPublisher;
    private final ObjectMapper objectMapper;

    public EventDeliveryReplayBizService(EventDeliveryService deliveryService,
                                         EventDeliveryReplayService replayService,
                                         EventMessageService messageService,
                                         EventTypeService eventTypeService,
                                         EventDeliveryTaskConverter taskConverter,
                                         EventDeliveryTaskPublisher taskPublisher,
                                         ObjectMapper objectMapper) {
        this.deliveryService = deliveryService;
        this.replayService = replayService;
        this.messageService = messageService;
        this.eventTypeService = eventTypeService;
        this.taskConverter = taskConverter;
        this.taskPublisher = taskPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * 重置失败记录并重新发布同一个稳定投递任务。
     *
     * @param eventId 事件全局标识及分片键
     * @param deliveryId 投递记录主键
     * @return 是否已提交人工重播任务
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean replayDelivery(String eventId, Long deliveryId) {
        EventDeliveryEntity delivery = deliveryService.requireDeliveryById(eventId, deliveryId);
        EventMessageEntity messageEntity = requireOriginalMessage(eventId, delivery);
        EventMessage message = restoreEventMessage(messageEntity);
        EventDeliveryTask task = taskConverter.convertDeliveryTask(message, delivery);
        replayService.resetDeliveryForReplay(eventId, deliveryId);
        taskPublisher.publishDeliveryTask(task);
        return true;
    }

    private EventMessageEntity requireOriginalMessage(
            String eventId,
            EventDeliveryEntity delivery) {
        EventMessageEntity message = messageService.fetchMessageEntityByEventId(eventId);
        if (message == null || !message.getId().equals(delivery.getEventMessageId())) {
            throw new ApiRuntimeException("投递任务对应的原始事件消息不存在。");
        }
        return message;
    }

    private EventMessage restoreEventMessage(EventMessageEntity messageEntity) {
        EventTypeEntity eventType = eventTypeService.requireEntity(messageEntity.getEventTypeId());
        return new EventMessage(
                messageEntity.getEventId(), eventType.getEventType(), messageEntity.getEventVersion(),
                messageEntity.getOccurredAt(), messageEntity.getSourceApplication(),
                messageEntity.getPartitionKey(), objectMapper.convertValue(
                        messageEntity.getEventContext(), new TypeReference<>() { }),
                messageEntity.getEventPayload());
    }
}
