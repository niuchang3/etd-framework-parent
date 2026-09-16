package org.etd.framework.starter.event.server.delivery.model;

import org.etd.framework.event.core.model.EventMessage;

import java.util.Objects;

/**
 * 一个原始事件面向一个业务订阅的 Kafka 内部投递任务。
 *
 * @param deliveryId 投递记录主键，所有重放过程保持不变
 * @param eventMessageId 原始事件消息记录主键
 * @param subscriptionId 目标订阅主键
 * @param eventId 原始事件全局标识及分片键
 * @param targetTopic 订阅创建任务时的目标 Topic 快照
 * @param message 需要原样投递的统一事件消息
 */
public record EventDeliveryTask(
        Long deliveryId,
        Long eventMessageId,
        Long subscriptionId,
        String eventId,
        String targetTopic,
        EventMessage message
) {

    public EventDeliveryTask {
        requirePositive(deliveryId, "投递任务主键必须大于 0");
        requirePositive(eventMessageId, "事件消息主键必须大于 0");
        requirePositive(subscriptionId, "订阅主键必须大于 0");
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("投递任务事件 ID 不能为空");
        }
        if (targetTopic == null || targetTopic.isBlank()) {
            throw new IllegalArgumentException("投递任务目标 Topic 不能为空");
        }
        message = Objects.requireNonNull(message, "投递任务事件消息不能为空");
        if (!eventId.equals(message.eventId())) {
            throw new IllegalArgumentException("投递任务事件 ID 必须与消息一致");
        }
    }

    private static void requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
