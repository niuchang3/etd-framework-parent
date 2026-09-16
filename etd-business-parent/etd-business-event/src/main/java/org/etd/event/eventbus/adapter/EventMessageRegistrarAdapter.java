package org.etd.event.eventbus.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.event.message.entity.EventMessageEntity;
import org.etd.event.message.mapper.EventMessageMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.eventbus.model.EventMessageRegistration;
import org.etd.framework.starter.event.server.eventbus.port.EventMessageRegistrar;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 基于事件消息表的事件总线幂等登记适配器。
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
     * 首次接收时创建消息记录，重复接收时校验事件内容未发生变化。
     *
     * @param message 统一事件消息
     * @param eventTypeId 事件类型主键
     * @return 消息登记结果
     */
    @Override
    public EventMessageRegistration registerEventMessage(
            EventMessage message, Long eventTypeId) {
        EventMessageEntity persisted = selectMessageByEventId(message.eventId());
        if (persisted != null) {
            ensureSameMessage(persisted, message, eventTypeId);
            return new EventMessageRegistration(persisted.getId(), false);
        }
        EventMessageEntity entity = createMessageEntity(message, eventTypeId);
        messageMapper.insert(entity);
        return new EventMessageRegistration(entity.getId(), true);
    }

    private EventMessageEntity selectMessageByEventId(String eventId) {
        LambdaQueryWrapper<EventMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventMessageEntity::getEventId, eventId);
        return messageMapper.selectOne(wrapper);
    }

    private void ensureSameMessage(
            EventMessageEntity persisted, EventMessage message, Long eventTypeId) {
        boolean sameMessage = persisted.getEventTypeId().equals(eventTypeId)
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

    private EventMessageEntity createMessageEntity(EventMessage message, Long eventTypeId) {
        EventMessageEntity entity = new EventMessageEntity();
        entity.setEventId(message.eventId());
        entity.setEventTypeId(eventTypeId);
        entity.setEventVersion(message.eventVersion());
        entity.setOccurredAt(message.occurredAt());
        entity.setSourceApplication(message.source());
        entity.setPartitionKey(message.partitionKey());
        entity.setEventContext(objectMapper.valueToTree(message.context()));
        entity.setEventPayload(message.payload());
        return entity;
    }
}
