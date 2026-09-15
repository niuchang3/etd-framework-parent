package org.etd.event.subscription.controller.vo;

import lombok.Data;

import java.time.Instant;

/**
 * 事件订阅页面响应对象。
 */
@Data
public class EventSubscriptionVO {

    /** 订阅配置主键。 */
    private Long id;

    /** 订阅配置的创建时间。 */
    private Instant createTime;

    /** 订阅配置的最后修改时间。 */
    private Instant updateTime;

    /** 订阅配置的乐观锁版本号。 */
    private Integer version;

    /** 订阅配置的显示名称。 */
    private String subscriptionName;

    /** 被订阅事件类型的主键。 */
    private Long eventTypeId;

    /** 被订阅事件类型的唯一编码。 */
    private String eventType;

    /** 被订阅事件类型的显示名称。 */
    private String eventName;

    /** 接收事件的业务应用标识。 */
    private String subscriberApplication;

    /** 服务端向业务应用发布事件的 Kafka Topic。 */
    private String targetTopic;

    /** 是否启用当前订阅配置。 */
    private Boolean enabled;

    /** 订阅配置的用途说明。 */
    private String description;
}
