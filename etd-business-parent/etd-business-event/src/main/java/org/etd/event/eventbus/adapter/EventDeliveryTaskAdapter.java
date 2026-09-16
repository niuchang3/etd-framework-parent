package org.etd.event.eventbus.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.etd.event.delivery.constant.EventDeliveryStatus;
import org.etd.event.delivery.converter.EventDeliveryTaskConverter;
import org.etd.event.delivery.entity.EventDeliveryEntity;
import org.etd.event.delivery.mapper.EventDeliveryMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryState;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryStatusEvent;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;
import org.etd.framework.starter.event.server.delivery.port.EventDeliveryTaskRegistrar;
import org.etd.framework.starter.event.server.eventbus.model.EventSubscriptionTarget;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于投递记录表的可靠投递任务登记与状态更新适配器。
 */
@Component
public class EventDeliveryTaskAdapter implements EventDeliveryTaskRegistrar {

    private final EventDeliveryMapper deliveryMapper;
    private final EventDeliveryTaskConverter taskConverter;

    public EventDeliveryTaskAdapter(EventDeliveryMapper deliveryMapper,
                                    EventDeliveryTaskConverter taskConverter) {
        this.deliveryMapper = deliveryMapper;
        this.taskConverter = taskConverter;
    }

    @Override
    public List<EventDeliveryTask> selectDeliveryTaskList(
            String eventId, Long messageId, EventMessage message) {
        LambdaQueryWrapper<EventDeliveryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventDeliveryEntity::getEventId, eventId)
                .eq(EventDeliveryEntity::getEventMessageId, messageId)
                .orderByAsc(EventDeliveryEntity::getId);
        return taskConverter.convertDeliveryTaskList(message, deliveryMapper.selectList(wrapper));
    }

    @Override
    public List<EventDeliveryTask> createDeliveryTaskList(
            EventMessage message,
            Long messageId,
            List<EventSubscriptionTarget> targetList) {
        List<EventDeliveryEntity> deliveryList = new ArrayList<>(targetList.size());
        for (EventSubscriptionTarget target : targetList) {
            EventDeliveryEntity delivery = createDelivery(message, messageId, target);
            deliveryMapper.insert(delivery);
            deliveryList.add(delivery);
        }
        return taskConverter.convertDeliveryTaskList(message, deliveryList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDeliveryStatus(EventDeliveryStatusEvent statusEvent) {
        EventDeliveryEntity current = requireDelivery(
                statusEvent.eventId(), statusEvent.deliveryId());
        requireSameTargetTopic(current, statusEvent);
        if (isObsoleteStatus(current, statusEvent)) {
            return;
        }
        LambdaUpdateWrapper<EventDeliveryEntity> wrapper = createStatusUpdate(current, statusEvent);
        if (deliveryMapper.update(null, wrapper) == 0) {
            throw new ApiRuntimeException("投递状态已被并发修改，等待状态消息重试。");
        }
    }

    private EventDeliveryEntity createDelivery(
            EventMessage message, Long messageId, EventSubscriptionTarget target) {
        EventDeliveryEntity delivery = new EventDeliveryEntity();
        delivery.setEventId(message.eventId());
        delivery.setEventMessageId(messageId);
        delivery.setSubscriptionId(target.subscriptionId());
        delivery.setTargetTopic(target.targetTopic());
        return delivery;
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

    private LambdaUpdateWrapper<EventDeliveryEntity> createStatusUpdate(
            EventDeliveryEntity current,
            EventDeliveryStatusEvent statusEvent) {
        LambdaUpdateWrapper<EventDeliveryEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EventDeliveryEntity::getEventId, statusEvent.eventId())
                .eq(EventDeliveryEntity::getId, statusEvent.deliveryId())
                .eq(EventDeliveryEntity::getVersion, current.getVersion())
                .set(EventDeliveryEntity::getDeliveryStatus, resolveStatusCode(statusEvent.state()))
                .set(EventDeliveryEntity::getAttemptCount, statusEvent.attempt())
                .set(EventDeliveryEntity::getVersion, current.getVersion() + 1);
        applyStatusMetadata(wrapper, statusEvent);
        return wrapper;
    }

    private void applyStatusMetadata(LambdaUpdateWrapper<EventDeliveryEntity> wrapper,
                                     EventDeliveryStatusEvent statusEvent) {
        switch (statusEvent.state()) {
            case PUBLISHING -> wrapper.set(EventDeliveryEntity::getLastError, null);
            case RETRY_WAITING -> wrapper
                    .set(EventDeliveryEntity::getNextRetryAt, statusEvent.nextRetryAt())
                    .set(EventDeliveryEntity::getLastError, statusEvent.errorMessage());
            case DEAD -> wrapper.set(EventDeliveryEntity::getLastError, statusEvent.errorMessage());
            case SUCCEEDED -> wrapper
                    .set(EventDeliveryEntity::getKafkaPartition, statusEvent.partition())
                    .set(EventDeliveryEntity::getKafkaOffset, statusEvent.offset())
                    .set(EventDeliveryEntity::getPublishedAt, statusEvent.publishedAt())
                    .set(EventDeliveryEntity::getLastError, null);
        }
    }

    private boolean isObsoleteStatus(EventDeliveryEntity current,
                                     EventDeliveryStatusEvent statusEvent) {
        if (current.getDeliveryStatus() == EventDeliveryStatus.SUCCEEDED.getCode()) {
            return true;
        }
        // 数据库事务回滚但 Kafka 重播事务已提交时，允许新一轮首次投递覆盖 DEAD 终态。
        if (isManualReplayStarting(current, statusEvent)) {
            return false;
        }
        if (statusEvent.attempt() < current.getAttemptCount()) {
            return true;
        }
        return current.getDeliveryStatus() == resolveStatusCode(statusEvent.state())
                && statusEvent.attempt() == current.getAttemptCount();
    }

    private boolean isManualReplayStarting(EventDeliveryEntity current,
                                           EventDeliveryStatusEvent statusEvent) {
        return current.getDeliveryStatus() == EventDeliveryStatus.DEAD.getCode()
                && statusEvent.state() == EventDeliveryState.PUBLISHING
                && statusEvent.attempt() == 1;
    }

    private void requireSameTargetTopic(EventDeliveryEntity current,
                                        EventDeliveryStatusEvent statusEvent) {
        if (!current.getTargetTopic().equals(statusEvent.targetTopic())) {
            throw new ApiRuntimeException("投递状态目标 Topic 与持久化任务不一致。");
        }
    }

    private int resolveStatusCode(EventDeliveryState state) {
        return switch (state) {
            case PUBLISHING -> EventDeliveryStatus.PUBLISHING.getCode();
            case SUCCEEDED -> EventDeliveryStatus.SUCCEEDED.getCode();
            case RETRY_WAITING -> EventDeliveryStatus.RETRY_WAITING.getCode();
            case DEAD -> EventDeliveryStatus.DEAD.getCode();
        };
    }
}
