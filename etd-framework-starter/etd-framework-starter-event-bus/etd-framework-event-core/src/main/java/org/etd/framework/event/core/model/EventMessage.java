package org.etd.framework.event.core.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 事件总线统一消息协议，发送端和消费端均以该模型作为最小消息单元。
 *
 * @param eventId      事件唯一标识
 * @param eventType    稳定的业务事件类型
 * @param eventVersion 事件数据协议版本
 * @param occurredAt   业务事件发生时间
 * @param source       事件来源应用
 * @param partitionKey Kafka 分区键
 * @param context      事件产生时的安全请求上下文
 * @param payload      JSON 事件主体
 */
public record EventMessage(
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String source,
        String partitionKey,
        Map<String, String> context,
        JsonNode payload
) {

    /**
     * 首个事件协议版本。
     */
    public static final int INITIAL_VERSION = 1;

    public EventMessage {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("事件 ID 不能为空");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("事件类型不能为空");
        }
        if (eventVersion <= 0) {
            throw new IllegalArgumentException("事件版本必须大于 0");
        }
        occurredAt = Objects.requireNonNull(occurredAt, "事件发生时间不能为空");
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("事件来源不能为空");
        }
        context = Map.copyOf(Objects.requireNonNull(context, "事件上下文不能为空"));
        payload = Objects.requireNonNull(payload, "事件主体不能为空");
    }
}
