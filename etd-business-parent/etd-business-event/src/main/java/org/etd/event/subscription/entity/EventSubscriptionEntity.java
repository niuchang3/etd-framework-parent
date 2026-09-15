package org.etd.event.subscription.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;

/**
 * 事件业务订阅实体，定义一个业务应用接收指定事件的 Kafka 投递目标。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "evt_event_subscription", excludeProperty = "tenantId")
public class EventSubscriptionEntity extends BaseEntity {

    /**
     * 数据库记录乐观锁版本。
     */
    @Version
    @TableField("version")
    private Integer version;

    /**
     * 订阅页面显示名称。
     */
    @TableField("subscription_name")
    private String subscriptionName;

    /**
     * 订阅的事件类型定义主键。
     */
    @TableField("event_type_id")
    private Long eventTypeId;

    /**
     * 实际处理该事件的业务应用名称。
     */
    @TableField("subscriber_application")
    private String subscriberApplication;

    /**
     * 业务应用独占的 Kafka 接收 Topic。
     */
    @TableField("target_topic")
    private String targetTopic;

    /**
     * 订阅业务用途说明。
     */
    @TableField("description")
    private String description;
}
