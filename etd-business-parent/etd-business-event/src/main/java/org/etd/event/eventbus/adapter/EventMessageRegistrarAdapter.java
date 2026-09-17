package org.etd.event.eventbus.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.event.message.entity.EventMessageEntity;
import org.etd.event.message.mapper.EventMessageMapper;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.eventbus.model.EventMessageRegistration;
import org.etd.framework.starter.event.server.eventbus.model.EventMessageStatus;
import org.etd.framework.starter.event.server.eventbus.port.EventMessageRegistrar;
import org.springframework.stereotype.Component;

/**
 * 基于事件消息表的入口消息查询与持久化适配器。
 */
@Component
public class EventMessageRegistrarAdapter implements EventMessageRegistrar {

    private final EventMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    public EventMessageRegistrarAdapter(EventMessageMapper messageMapper, ObjectMapper objectMapper) {
        this.messageMapper = messageMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询已经持久化的入口消息并转换为 starter-server 统一模型。
     *
     * @param eventId 事件全局唯一标识及分片键
     * @return 已持久化消息快照，不存在时返回 {@code null}
     */
    @Override
    public EventMessageRegistration selectEventMessage(String eventId) {
        EventMessageEntity entity = selectMessageByEventId(eventId);
        if (entity == null) {
            return null;
        }
        EventMessage message = restoreEventMessage(entity);
        EventMessageStatus status = EventMessageStatus.fromCode(entity.getMessageStatus());
        return new EventMessageRegistration(
                entity.getId(), message, entity.getEventTypeId(), status,
                entity.getFailureReason());
    }

    /**
     * 按 starter-server 给出的最终处理结果创建入口消息。
     *
     * @param message 原始事件消息
     * @param eventTypeId 事件类型主键，类型无法解析时为空
     * @param status 消息处理状态
     * @param failureReason 业务校验失败原因，正常消息为空
     * @return 新建消息的持久化标识
     */
    @Override
    public Long createEventMessage(EventMessage message,
                                   Long eventTypeId,
                                   EventMessageStatus status,
                                   String failureReason) {
        EventMessageEntity entity = createMessageEntity(
                message, eventTypeId, status, failureReason);
        messageMapper.insert(entity);
        return entity.getId();
    }

    private EventMessageEntity selectMessageByEventId(String eventId) {
        LambdaQueryWrapper<EventMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventMessageEntity::getEventId, eventId);
        return messageMapper.selectOne(wrapper);
    }

    private EventMessageEntity createMessageEntity(
            EventMessage message,
            Long eventTypeId,
            EventMessageStatus status,
            String failureReason) {
        EventMessageEntity entity = new EventMessageEntity();
        entity.setEventId(message.eventId());
        entity.setEventType(message.eventType());
        entity.setEventTypeId(eventTypeId);
        entity.setEventVersion(message.eventVersion());
        entity.setOccurredAt(message.occurredAt());
        entity.setSourceApplication(message.source());
        entity.setPartitionKey(message.partitionKey());
        entity.setEventContext(objectMapper.valueToTree(message.context()));
        entity.setEventPayload(message.payload());
        entity.setMessageStatus(status.getCode());
        entity.setFailureReason(failureReason);
        return entity;
    }

    private EventMessage restoreEventMessage(EventMessageEntity entity) {
        return new EventMessage(
                entity.getEventId(), entity.getEventType(), entity.getEventVersion(),
                entity.getOccurredAt(), entity.getSourceApplication(), entity.getPartitionKey(),
                objectMapper.convertValue(entity.getEventContext(), new TypeReference<>() { }),
                entity.getEventPayload());
    }
}
