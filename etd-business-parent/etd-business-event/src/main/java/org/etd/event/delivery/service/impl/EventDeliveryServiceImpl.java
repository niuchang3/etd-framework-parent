package org.etd.event.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.event.delivery.constant.EventDeliveryStatus;
import org.etd.event.delivery.controller.vo.EventDeliveryVO;
import org.etd.event.delivery.entity.EventDeliveryEntity;
import org.etd.event.delivery.mapper.EventDeliveryMapper;
import org.etd.event.delivery.service.EventDeliveryService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 事件投递任务查询与人工重播能力实现。
 */
@Service
public class EventDeliveryServiceImpl implements EventDeliveryService {

    private final EventDeliveryMapper deliveryMapper;

    public EventDeliveryServiceImpl(EventDeliveryMapper deliveryMapper) {
        this.deliveryMapper = deliveryMapper;
    }

    @Override
    public IPage<EventDeliveryVO> page(long current, long size, String eventId, Long subscriptionId,
                                       Integer deliveryStatus, Instant startTime, Instant endTime) {
        return deliveryMapper.selectDeliveryPage(new Page<>(current, size), eventId,
                subscriptionId, deliveryStatus, startTime, endTime);
    }

    @Override
    public EventDeliveryVO fetchByShardingKeyAndId(Short shardingKey, Long id) {
        return toVO(requireDelivery(shardingKey, id));
    }

    @Override
    public List<EventDeliveryVO> selectListByMessage(Short shardingKey, Long eventMessageId) {
        return deliveryMapper.selectListByMessage(shardingKey, eventMessageId);
    }

    @Override
    public boolean replayFailedDelivery(Short shardingKey, Long id) {
        EventDeliveryEntity current = requireDelivery(shardingKey, id);
        ensureReplayable(current);
        LambdaUpdateWrapper<EventDeliveryEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EventDeliveryEntity::getShardingKey, shardingKey)
                .eq(EventDeliveryEntity::getId, id)
                .eq(EventDeliveryEntity::getVersion, current.getVersion())
                .in(EventDeliveryEntity::getDeliveryStatus,
                        EventDeliveryStatus.RETRY_WAITING.getCode(), EventDeliveryStatus.DEAD.getCode())
                .set(EventDeliveryEntity::getDeliveryStatus, EventDeliveryStatus.PENDING.getCode())
                .set(EventDeliveryEntity::getNextRetryAt, Instant.now())
                .set(EventDeliveryEntity::getLastError, null)
                .set(EventDeliveryEntity::getKafkaPartition, null)
                .set(EventDeliveryEntity::getKafkaOffset, null)
                .set(EventDeliveryEntity::getPublishedAt, null)
                .set(EventDeliveryEntity::getVersion, current.getVersion() + 1);
        if (deliveryMapper.update(null, wrapper) == 0) {
            throw new ApiRuntimeException("投递任务状态已变化，请刷新后重试。");
        }
        return true;
    }

    private EventDeliveryEntity requireDelivery(Short shardingKey, Long id) {
        LambdaQueryWrapper<EventDeliveryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventDeliveryEntity::getShardingKey, shardingKey)
                .eq(EventDeliveryEntity::getId, id);
        EventDeliveryEntity entity = deliveryMapper.selectOne(wrapper);
        if (entity == null) {
            throw new ApiRuntimeException("事件投递任务不存在。");
        }
        return entity;
    }

    private void ensureReplayable(EventDeliveryEntity entity) {
        int status = entity.getDeliveryStatus();
        if (status != EventDeliveryStatus.RETRY_WAITING.getCode()
                && status != EventDeliveryStatus.DEAD.getCode()) {
            throw new ApiRuntimeException("仅等待重试或死信状态的投递任务允许人工重播。");
        }
    }

    private EventDeliveryVO toVO(EventDeliveryEntity entity) {
        EventDeliveryVO vo = new EventDeliveryVO();
        vo.setId(entity.getId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setVersion(entity.getVersion());
        vo.setEventId(entity.getEventId());
        vo.setShardingKey(entity.getShardingKey());
        vo.setEventMessageId(entity.getEventMessageId());
        vo.setSubscriptionId(entity.getSubscriptionId());
        vo.setTargetTopic(entity.getTargetTopic());
        vo.setDeliveryStatus(entity.getDeliveryStatus());
        vo.setAttemptCount(entity.getAttemptCount());
        vo.setNextRetryAt(entity.getNextRetryAt());
        vo.setLastError(entity.getLastError());
        vo.setKafkaPartition(entity.getKafkaPartition());
        vo.setKafkaOffset(entity.getKafkaOffset());
        vo.setPublishedAt(entity.getPublishedAt());
        return vo;
    }
}
