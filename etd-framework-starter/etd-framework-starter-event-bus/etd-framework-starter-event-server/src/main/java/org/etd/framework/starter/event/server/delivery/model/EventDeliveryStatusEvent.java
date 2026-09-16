package org.etd.framework.starter.event.server.delivery.model;

import java.time.Instant;
import java.util.Objects;

/**
 * 投递状态 Topic 使用的内部状态变更协议。
 *
 * @param deliveryId 投递记录主键
 * @param eventId 原始事件全局标识及分片键
 * @param state 最新投递状态
 * @param attempt 当前投递尝试次数，从 1 开始
 * @param targetTopic 实际目标 Topic
 * @param partition 成功发送后的目标分区
 * @param offset 成功发送后的目标偏移量
 * @param nextRetryAt 下一次容器重试的预计时间
 * @param publishedAt 成功发送到目标 Topic 的时间
 * @param errorMessage 最近一次失败摘要
 */
public record EventDeliveryStatusEvent(
        Long deliveryId,
        String eventId,
        EventDeliveryState state,
        int attempt,
        String targetTopic,
        Integer partition,
        Long offset,
        Instant nextRetryAt,
        Instant publishedAt,
        String errorMessage
) {

    public EventDeliveryStatusEvent {
        if (deliveryId == null || deliveryId <= 0) {
            throw new IllegalArgumentException("投递状态主键必须大于 0");
        }
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("投递状态事件 ID 不能为空");
        }
        state = Objects.requireNonNull(state, "投递状态不能为空");
        if (attempt <= 0) {
            throw new IllegalArgumentException("投递尝试次数必须大于 0");
        }
        if (targetTopic == null || targetTopic.isBlank()) {
            throw new IllegalArgumentException("投递状态目标 Topic 不能为空");
        }
        validateStateMetadata(state, partition, offset, nextRetryAt, publishedAt);
    }

    private static void validateStateMetadata(EventDeliveryState state,
                                              Integer partition,
                                              Long offset,
                                              Instant nextRetryAt,
                                              Instant publishedAt) {
        if (state == EventDeliveryState.SUCCEEDED
                && (partition == null || offset == null || publishedAt == null)) {
            throw new IllegalArgumentException("成功投递状态必须包含 Kafka 元数据和发布时间");
        }
        if (state == EventDeliveryState.RETRY_WAITING && nextRetryAt == null) {
            throw new IllegalArgumentException("等待重试状态必须包含下次重试时间");
        }
    }
}
