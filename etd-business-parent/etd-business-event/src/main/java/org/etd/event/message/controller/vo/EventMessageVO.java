package org.etd.event.message.controller.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.Instant;

/**
 * 事件消息页面响应对象，原始载荷仅在详情接口返回。
 */
@Data
public class EventMessageVO {
    private Long id;
    private Instant createTime;
    private Integer version;
    private Short shardingKey;
    private String eventId;
    private Long eventTypeId;
    private Integer eventVersion;
    private Instant occurredAt;
    private String sourceApplication;
    private String partitionKey;
    private JsonNode eventContext;
    private JsonNode eventPayload;
}
