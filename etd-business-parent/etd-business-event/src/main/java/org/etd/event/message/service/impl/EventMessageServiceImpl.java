package org.etd.event.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.event.message.controller.vo.EventMessageVO;
import org.etd.event.message.entity.EventMessageEntity;
import org.etd.event.message.mapper.EventMessageMapper;
import org.etd.event.message.service.EventMessageService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.event.server.eventbus.model.EventMessageStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;

/**
 * 持久化事件消息基础读写能力实现。
 */
@Service
public class EventMessageServiceImpl implements EventMessageService {

    private final EventMessageMapper messageMapper;

    public EventMessageServiceImpl(EventMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public IPage<EventMessageVO> page(
            long current, long size, String eventId, String eventType,
            Long eventTypeId, EventMessageStatus messageStatus,
            String sourceApplication, Instant startTime, Instant endTime) {
        Integer messageStatusCode = messageStatus == null ? null : messageStatus.getCode();
        LambdaQueryWrapper<EventMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(eventId), EventMessageEntity::getEventId, eventId)
                .eq(StringUtils.hasText(eventType), EventMessageEntity::getEventType, eventType)
                .eq(eventTypeId != null, EventMessageEntity::getEventTypeId, eventTypeId)
                .eq(messageStatusCode != null,
                        EventMessageEntity::getMessageStatus, messageStatusCode)
                .eq(StringUtils.hasText(sourceApplication),
                        EventMessageEntity::getSourceApplication, sourceApplication)
                .ge(startTime != null, EventMessageEntity::getCreateTime, startTime)
                .lt(endTime != null, EventMessageEntity::getCreateTime, endTime)
                .orderByDesc(EventMessageEntity::getCreateTime)
                .orderByDesc(EventMessageEntity::getId);
        return messageMapper.selectPage(new Page<>(current, size), wrapper).convert(this::toVOWithoutPayload);
    }

    @Override
    public EventMessageVO fetchMessageById(String eventId, Long messageId) {
        LambdaQueryWrapper<EventMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventMessageEntity::getEventId, eventId)
                .eq(EventMessageEntity::getId, messageId);
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

    private EventMessageVO toVOWithoutPayload(EventMessageEntity entity) {
        return toVO(entity, false);
    }

    private EventMessageVO toVO(EventMessageEntity entity, boolean includePayload) {
        EventMessageVO vo = new EventMessageVO();
        vo.setId(entity.getId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setVersion(entity.getVersion());
        vo.setEventId(entity.getEventId());
        vo.setEventType(entity.getEventType());
        vo.setEventTypeId(entity.getEventTypeId());
        vo.setEventVersion(entity.getEventVersion());
        vo.setOccurredAt(entity.getOccurredAt());
        vo.setSourceApplication(entity.getSourceApplication());
        vo.setPartitionKey(entity.getPartitionKey());
        vo.setMessageStatus(EventMessageStatus.fromCode(entity.getMessageStatus()));
        vo.setFailureReason(entity.getFailureReason());
        if (includePayload) {
            vo.setEventContext(entity.getEventContext());
            vo.setEventPayload(entity.getEventPayload());
        }
        return vo;
    }
}
