package org.etd.event.delivery.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;

import java.time.Instant;

/**
 * 事件订阅投递任务实体，每条记录独立描述一个事件向一个业务订阅的 Kafka 发布结果。
 *
 * <p>投递记录复制原始消息的 {@code eventId} 和 {@code shardingKey}，确保两张逻辑表
 * 始终路由到编号相同的物理分表，并支持针对具体订阅独立重试和重播。</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "evt_event_delivery",
        excludeProperty = {"tenantId", "dataStatus", "createBy", "updateBy"})
public class EventDeliveryEntity extends BaseEntity {

    /**
     * 投递任务并发更新使用的乐观锁版本。
     */
    @Version
    @TableField(value = "version", fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    private Integer version;

    /**
     * 原始事件全局标识，用于分片定位和业务幂等追踪。
     */
    @TableField("event_id")
    private String eventId;

    /**
     * 从原始事件复制的固定分片编号。
     */
    @TableField("sharding_key")
    private Short shardingKey;

    /**
     * 原始事件消息主键。
     */
    @TableField("event_message_id")
    private Long eventMessageId;

    /**
     * 目标业务订阅主键。
     */
    @TableField("subscription_id")
    private Long subscriptionId;

    /**
     * 创建投递任务时的 Kafka Topic 快照。
     */
    @TableField("target_topic")
    private String targetTopic;

    /**
     * 当前投递状态，默认等待投递。
     */
    @TableField(value = "delivery_status", fill = FieldFill.INSERT)
    @TableFieldExtend("T(org.etd.event.delivery.constant.EventDeliveryStatus).PENDING.getCode()")
    private Integer deliveryStatus;

    /**
     * 累计投递次数，首次创建任务时为零。
     */
    @TableField(value = "attempt_count", fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    private Integer attemptCount;

    /**
     * 下次允许自动投递或重试的时间点。
     */
    @TableField(value = "next_retry_at", fill = FieldFill.INSERT)
    @TableFieldExtend("T(java.time.Instant).now()")
    private Instant nextRetryAt;

    /**
     * 最近一次 Kafka 发布失败的错误摘要，不允许保存凭证等敏感信息。
     */
    @TableField("last_error")
    private String lastError;

    /**
     * Kafka 成功发送后返回的目标分区。
     */
    @TableField("kafka_partition")
    private Integer kafkaPartition;

    /**
     * Kafka 成功发送后返回的消息偏移量。
     */
    @TableField("kafka_offset")
    private Long kafkaOffset;

    /**
     * 成功发布到目标 Kafka Topic 的时间点。
     */
    @TableField("published_at")
    private Instant publishedAt;
}
