package org.etd.event.delivery.controller.vo;

import lombok.Data;

import java.time.Instant;

/**
 * 事件投递任务页面响应对象，包含目标订阅快照和当前发布结果。
 */
@Data
public class EventDeliveryVO {
    private Long id;
    private Instant createTime;
    private Instant updateTime;
    private Integer version;
    private String eventId;
    private Short shardingKey;
    private Long eventMessageId;
    private Long subscriptionId;
    private String subscriptionCode;
    private String subscriptionName;
    private String subscriberApplication;
    private String consumerGroup;
    private String targetTopic;
    private Integer deliveryStatus;
    private Integer attemptCount;
    private Instant nextRetryAt;
    private String lastError;
    private Integer kafkaPartition;
    private Long kafkaOffset;
    private Instant publishedAt;
}
