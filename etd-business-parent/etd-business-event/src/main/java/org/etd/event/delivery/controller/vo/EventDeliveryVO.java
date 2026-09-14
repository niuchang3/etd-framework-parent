package org.etd.event.delivery.controller.vo;

import lombok.Data;

import java.time.Instant;

/**
 * 事件投递任务页面响应对象，包含目标订阅快照和当前发布结果。
 */
@Data
public class EventDeliveryVO {

    /** 投递任务主键。 */
    private Long id;

    /** 投递任务的创建时间。 */
    private Instant createTime;

    /** 投递任务的最后更新时间。 */
    private Instant updateTime;

    /** 投递任务的乐观锁版本号。 */
    private Integer version;

    /** 原始事件全局标识，同时作为投递表的分片键。 */
    private String eventId;

    /** 原始事件消息的主键。 */
    private Long eventMessageId;

    /** 目标订阅配置的主键。 */
    private Long subscriptionId;

    /** 目标订阅编码快照。 */
    private String subscriptionCode;

    /** 目标订阅的显示名称。 */
    private String subscriptionName;

    /** 接收本次投递的业务应用标识。 */
    private String subscriberApplication;

    /** 业务客户端消费目标 Topic 时使用的消费组。 */
    private String consumerGroup;

    /** 本次投递使用的 Kafka Topic 快照。 */
    private String targetTopic;

    /** 当前投递状态码。 */
    private Integer deliveryStatus;

    /** 已执行的投递次数。 */
    private Integer attemptCount;

    /** 下一次允许重试的时间。 */
    private Instant nextRetryAt;

    /** 最近一次投递失败的错误摘要。 */
    private String lastError;

    /** Kafka 发布成功后返回的分区编号。 */
    private Integer kafkaPartition;

    /** Kafka 发布成功后返回的消息偏移量。 */
    private Long kafkaOffset;

    /** 最近一次成功发布到 Kafka 的时间。 */
    private Instant publishedAt;
}
