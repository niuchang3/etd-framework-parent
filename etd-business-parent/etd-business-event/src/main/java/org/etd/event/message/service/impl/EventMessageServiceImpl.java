package org.etd.event.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.event.message.controller.vo.EventMessageVO;
import org.etd.event.message.entity.EventMessageEntity;
import org.etd.event.message.mapper.EventMessageMapper;
import org.etd.event.message.service.EventMessageService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.event.core.model.EventMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;

/**
 * 持久化事件消息只读查询能力实现。
 */
@Service
public class EventMessageServiceImpl implements EventMessageService {

    private final EventMessageMapper messageMapper;

    private final ObjectMapper objectMapper;

    public EventMessageServiceImpl(EventMessageMapper messageMapper, ObjectMapper objectMapper) {
        this.messageMapper = messageMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public IPage<EventMessageVO> page(long current, long size, String eventId, Long eventTypeId,
                                      String sourceApplication, Instant startTime, Instant endTime) {
        LambdaQueryWrapper<EventMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(eventId), EventMessageEntity::getEventId, eventId)
                .eq(eventTypeId != null, EventMessageEntity::getEventTypeId, eventTypeId)
                .eq(StringUtils.hasText(sourceApplication), EventMessageEntity::getSourceApplication, sourceApplication)
                .ge(startTime != null, EventMessageEntity::getCreateTime, startTime)
                .lt(endTime != null, EventMessageEntity::getCreateTime, endTime)
                .orderByDesc(EventMessageEntity::getCreateTime)
                .orderByDesc(EventMessageEntity::getId);
        return messageMapper.selectPage(new Page<>(current, size), wrapper).convert(this::toVOWithoutPayload);
    }

    @Override
    public EventMessageVO fetchByEventIdAndId(String eventId, Long id) {
        LambdaQueryWrapper<EventMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventMessageEntity::getEventId, eventId)
                .eq(EventMessageEntity::getId, id);
        EventMessageEntity entity = messageMapper.selectOne(wrapper);
        if (entity == null) {
            throw new ApiRuntimeException("事件消息不存在。");
        }
        return toVO(entity, true);
    }

    @Override
    public EventMessageEntity fetchMessageEntityByEventId(String eventId) {
        LambdaQueryWrapper<EventMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventMessageEntity::getEventId, eventId);
        return messageMapper.selectOne(wrapper);
    }

    @Override
    public Long createEventMessage(EventMessage message, Long eventTypeId) {
        EventMessageEntity entity = new EventMessageEntity();
        entity.setEventId(message.eventId());
        entity.setEventTypeId(eventTypeId);
        entity.setEventVersion(message.eventVersion());
        entity.setOccurredAt(message.occurredAt());
        entity.setSourceApplication(message.source());
        entity.setPartitionKey(message.partitionKey());
        entity.setEventContext(objectMapper.valueToTree(message.context()));
        entity.setEventPayload(message.payload());
        messageMapper.insert(entity);
        return entity.getId();
    }

    private EventMessageVO toVOWithoutPayload(EventMessageEntity entity) {
        return toVO(entity, false);
    }

    private EventMessageVO toVO(EventMessageEntity entity, boolean includePayload) {
        EventMessageVO vo = new EventMessageVO();
        vo.setId(entity.getId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setVersion(entity.getVersion());
        vo.setEventId(entity.getEventId());
        vo.setEventTypeId(entity.getEventTypeId());
        vo.setEventVersion(entity.getEventVersion());
        vo.setOccurredAt(entity.getOccurredAt());
        vo.setSourceApplication(entity.getSourceApplication());
        vo.setPartitionKey(entity.getPartitionKey());
        if (includePayload) {
            vo.setEventContext(entity.getEventContext());
            vo.setEventPayload(entity.getEventPayload());
        }
        return vo;
    }
}
