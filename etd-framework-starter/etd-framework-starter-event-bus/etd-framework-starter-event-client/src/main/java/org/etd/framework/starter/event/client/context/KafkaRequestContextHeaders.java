package org.etd.framework.starter.event.client.context;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.etd.framework.common.core.context.RequestContextInitializer;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kafka Header 与框架请求上下文之间的转换器。
 */
public final class KafkaRequestContextHeaders {

    private KafkaRequestContextHeaders() {
    }

    /**
     * 将当前请求上下文写入 Kafka Header。
     *
     * @param headers Kafka 消息头
     */
    public static void writeTo(Headers headers) {
        Map<String, Object> contextHeaders = RequestContextInitializer.exportMessageHeaders();
        contextHeaders.forEach((name, value) -> writeHeader(headers, name, value));
    }

    /**
     * 从 Kafka Header 还原请求上下文所需的字符串 Map。
     *
     * @param headers Kafka 消息头
     * @return 请求上下文消息头
     */
    public static Map<String, Object> readFrom(Headers headers) {
        Map<String, Object> contextHeaders = new LinkedHashMap<>();
        for (Header header : headers) {
            if (header.value() != null) {
                contextHeaders.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
            }
        }
        return contextHeaders;
    }

    private static void writeHeader(Headers headers, String name, Object value) {
        if (value == null) {
            return;
        }
        headers.remove(name);
        headers.add(name, String.valueOf(value).getBytes(StandardCharsets.UTF_8));
    }
}
