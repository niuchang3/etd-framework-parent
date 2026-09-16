package org.etd.framework.event.core.sender;

/**
 * 事件发送完成后的 Kafka 元数据结果。
 *
 * @param eventId    事件唯一标识
 * @param destination 实际发送目的地
 * @param partition Kafka 目标分区
 * @param offset Kafka 消息偏移量
 */
public record EventSendResult(String eventId, String destination, int partition, long offset) {

    public EventSendResult {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("事件 ID 不能为空");
        }
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("事件发送目的地不能为空");
        }
        if (partition < 0) {
            throw new IllegalArgumentException("Kafka 目标分区不能小于 0");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Kafka 消息偏移量不能小于 0");
        }
    }
}
