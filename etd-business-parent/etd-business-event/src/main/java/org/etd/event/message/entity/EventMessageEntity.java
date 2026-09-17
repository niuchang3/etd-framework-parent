package org.etd.event.message.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;

import java.time.Instant;

/**
 * 原始事件消息实体，完整保存进入事件中心的统一事件协议内容。
 *
 * <p>原始消息字段写入后不可修改，{@code eventId} 直接作为 ShardingSphere
 * 分表路由键，同一个事件标识始终进入同一张物理表。</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "evt_event_message", autoResultMap = true,
        excludeProperty = {"tenantId", "dataStatus", "createBy", "updateBy"})
public class EventMessageEntity extends BaseEntity {

    /**
     * 数据库记录乐观锁版本。
     */
    @Version
    @TableField("version")
    private Integer version;

    /**
     * 事件全局唯一标识，同时用于幂等判断和分片计算。
     */
    @TableField("event_id")
    private String eventId;

    /**
     * 消息携带的原始事件类型编码，类型解析失败时仍完整保留。
     */
    @TableField("event_type")
    private String eventType;

    /**
     * 事件类型定义主键，类型不存在时为空。
     */
    @TableField("event_type_id")
    private Long eventTypeId;

    /**
     * 当前消息使用的事件协议版本。
     */
    @TableField("event_version")
    private Integer eventVersion;

    /**
     * 业务事件实际发生时间。
     */
    @TableField("occurred_at")
    private Instant occurredAt;

    /**
     * 实际发送事件的来源应用。
     */
    @TableField("source_application")
    private String sourceApplication;

    /**
     * 保证同一业务聚合事件有序性的 Kafka 分区键。
     */
    @TableField("partition_key")
    private String partitionKey;

    /**
     * 事件产生时需要跨服务传递的安全请求上下文。
     */
    @TableField(value = "event_context", typeHandler = JacksonTypeHandler.class)
    private JsonNode eventContext;

    /**
     * 事件业务数据，不允许在事件中心内修改业务字段。
     */
    @TableField(value = "event_payload", typeHandler = JacksonTypeHandler.class)
    private JsonNode eventPayload;

    /**
     * 入口消息处理状态：1 表示正常，0 表示业务校验失败。
     */
    @TableField("message_status")
    private Integer messageStatus;

    /**
     * 业务校验失败原因，正常消息为空。
     */
    @TableField("failure_reason")
    private String failureReason;
}
