package org.etd.event.message.controller.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.etd.framework.starter.event.server.eventbus.model.EventMessageStatus;

import java.time.Instant;

/**
 * 事件消息页面响应对象，原始载荷仅在详情接口返回。
 */
@Data
public class EventMessageVO {

    /** 消息记录主键。 */
    private Long id;

    /** 服务端接收并持久化消息的时间。 */
    private Instant createTime;

    /** 消息记录的乐观锁版本号。 */
    private Integer version;

    /** 事件全局唯一标识，同时作为消息表和投递表的分片键。 */
    private String eventId;

    /** 消息携带的原始事件类型编码。 */
    private String eventType;

    /** 关联的事件类型主键，类型解析失败时为空。 */
    private Long eventTypeId;

    /** 当前消息使用的事件协议版本。 */
    private Integer eventVersion;

    /** 业务事件实际发生的时间。 */
    private Instant occurredAt;

    /** 发送事件的业务应用标识。 */
    private String sourceApplication;

    /** 发送端指定的 Kafka 分区键。 */
    private String partitionKey;

    /** 事件携带的请求链路上下文。 */
    private JsonNode eventContext;

    /** 原始事件业务载荷。 */
    private JsonNode eventPayload;

    /** 入口消息处理状态，异常消息不会生成后续投递任务。 */
    private EventMessageStatus messageStatus;

    /** 业务校验失败原因，正常消息为空。 */
    private String failureReason;
}
