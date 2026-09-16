package org.etd.event.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.etd.event.delivery.constant.EventDeliveryStatus;
import org.etd.event.delivery.entity.EventDeliveryEntity;
import org.etd.event.delivery.mapper.EventDeliveryMapper;
import org.etd.event.delivery.service.EventDeliveryReplayService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 失败投递记录的人工重播状态重置实现。
 */
@Service
public class EventDeliveryReplayServiceImpl implements EventDeliveryReplayService {

    private final EventDeliveryMapper deliveryMapper;

    public EventDeliveryReplayServiceImpl(EventDeliveryMapper deliveryMapper) {
        this.deliveryMapper = deliveryMapper;
    }

    @Override
    public void resetDeliveryForReplay(String eventId, Long deliveryId) {
        EventDeliveryEntity current = requireDelivery(eventId, deliveryId);
        requireReplayableDelivery(current);
        LambdaUpdateWrapper<EventDeliveryEntity> wrapper = createReplayUpdate(current);
        if (deliveryMapper.update(null, wrapper) == 0) {
            throw new ApiRuntimeException("投递任务状态已变化，请刷新后重试。");
        }
    }

    private EventDeliveryEntity requireDelivery(String eventId, Long deliveryId) {
        LambdaQueryWrapper<EventDeliveryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventDeliveryEntity::getEventId, eventId)
                .eq(EventDeliveryEntity::getId, deliveryId);
        EventDeliveryEntity delivery = deliveryMapper.selectOne(wrapper);
        if (delivery == null) {
            throw new ApiRuntimeException("事件投递任务不存在。");
        }
        return delivery;
    }

    private LambdaUpdateWrapper<EventDeliveryEntity> createReplayUpdate(
            EventDeliveryEntity current) {
        return new LambdaUpdateWrapper<EventDeliveryEntity>()
                .eq(EventDeliveryEntity::getEventId, current.getEventId())
                .eq(EventDeliveryEntity::getId, current.getId())
                .eq(EventDeliveryEntity::getVersion, current.getVersion())
                .in(EventDeliveryEntity::getDeliveryStatus,
                        EventDeliveryStatus.RETRY_WAITING.getCode(), EventDeliveryStatus.DEAD.getCode())
                .set(EventDeliveryEntity::getDeliveryStatus, EventDeliveryStatus.PENDING.getCode())
                .set(EventDeliveryEntity::getAttemptCount, 0)
                .set(EventDeliveryEntity::getNextRetryAt, Instant.now())
                .set(EventDeliveryEntity::getLastError, null)
                .set(EventDeliveryEntity::getKafkaPartition, null)
                .set(EventDeliveryEntity::getKafkaOffset, null)
                .set(EventDeliveryEntity::getPublishedAt, null)
                .set(EventDeliveryEntity::getVersion, current.getVersion() + 1);
    }

    private void requireReplayableDelivery(EventDeliveryEntity delivery) {
        int status = delivery.getDeliveryStatus();
        if (status != EventDeliveryStatus.RETRY_WAITING.getCode()
                && status != EventDeliveryStatus.DEAD.getCode()) {
            throw new ApiRuntimeException("仅等待重试或死信状态的投递任务允许人工重播。");
        }
    }
}
