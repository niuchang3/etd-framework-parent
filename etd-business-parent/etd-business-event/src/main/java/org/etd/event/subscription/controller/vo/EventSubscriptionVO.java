package org.etd.event.subscription.controller.vo;

import lombok.Data;

import java.time.Instant;

/**
 * 事件订阅页面响应对象。
 */
@Data
public class EventSubscriptionVO {
    private Long id;
    private Instant createTime;
    private Instant updateTime;
    private Integer version;
    private String subscriptionCode;
    private String subscriptionName;
    private Long eventTypeId;
    private String eventType;
    private String eventName;
    private String subscriberApplication;
    private String targetTopic;
    private String consumerGroup;
    private Boolean enabled;
    private String description;
}
