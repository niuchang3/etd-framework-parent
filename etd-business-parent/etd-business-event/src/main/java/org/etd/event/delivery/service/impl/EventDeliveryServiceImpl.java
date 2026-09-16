package org.etd.event.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.event.delivery.controller.vo.EventDeliveryVO;
import org.etd.event.delivery.entity.EventDeliveryEntity;
import org.etd.event.delivery.mapper.EventDeliveryMapper;
import org.etd.event.delivery.service.EventDeliveryService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 事件投递记录查询能力实现。
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
    public EventDeliveryVO fetchDeliveryById(String eventId, Long deliveryId) {
        return toVO(requireDelivery(eventId, deliveryId));
    }

    @Override
    public EventDeliveryEntity requireDeliveryById(String eventId, Long deliveryId) {
        return requireDelivery(eventId, deliveryId);
    }

    @Override
    public List<EventDeliveryVO> selectDeliveryListByMessageId(String eventId, Long eventMessageId) {
        return deliveryMapper.selectDeliveryListByMessageId(eventId, eventMessageId);
    }

    private EventDeliveryEntity requireDelivery(String eventId, Long id) {
        LambdaQueryWrapper<EventDeliveryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventDeliveryEntity::getEventId, eventId)
                .eq(EventDeliveryEntity::getId, id);
        EventDeliveryEntity entity = deliveryMapper.selectOne(wrapper);
        if (entity == null) {
            throw new ApiRuntimeException("事件投递任务不存在。");
        }
        return entity;
    }

    private EventDeliveryVO toVO(EventDeliveryEntity entity) {
        EventDeliveryVO vo = new EventDeliveryVO();
        vo.setId(entity.getId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setVersion(entity.getVersion());
        vo.setEventId(entity.getEventId());
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
