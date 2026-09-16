package org.etd.event.delivery.constant;

/**
 * 事件投递任务状态，状态码会持久化到 {@code evt_event_delivery.delivery_status}。
 */
public enum EventDeliveryStatus {

    /**
     * 等待投递。
     */
    PENDING(0),

    /**
     * 已被 Kafka 投递任务消费者领取并正在发布。
     */
    PUBLISHING(1),

    /**
     * 已成功发布到目标 Kafka Topic。
     */
    SUCCEEDED(2),

    /**
     * 本次发布失败，等待下一次自动重试。
     */
    RETRY_WAITING(3),

    /**
     * 超过最大重试限制，等待人工处理或重播。
     */
    DEAD(4);

    private final int code;

    EventDeliveryStatus(int code) {
        this.code = code;
    }

    /**
     * 获取数据库持久化状态码。
     *
     * @return 状态码
     */
    public int getCode() {
        return code;
    }
}
