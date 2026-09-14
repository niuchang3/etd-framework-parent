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
 * <p>原始消息字段写入后不可修改，{@code shardingKey} 由 {@code eventId}
 * 按框架统一算法计算，并作为 ShardingSphere 固定分表路由键。</p>
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
     * 根据事件全局标识计算得到的固定分片编号。
     */
    @TableField("sharding_key")
    private Short shardingKey;

    /**
     * 事件全局唯一标识，同时用于幂等判断和分片计算。
     */
    @TableField("event_id")
    private String eventId;

    /**
     * 事件类型定义主键。
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
}
