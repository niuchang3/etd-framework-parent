package org.etd.framework.event.core.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.etd.framework.event.core.model.EventMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一事件消息 JSON 编解码器，保证发送与转发使用相同的线协议。
 */
public class EventMessageCodec {

    private final ObjectReader eventMessageReader;

    private final ObjectWriter eventMessageWriter;

    public EventMessageCodec(ObjectMapper objectMapper) {
        // Event Bus 明确忽略未知字段，避免业务应用的全局 Jackson 配置改变协议升级策略。
        this.eventMessageReader = objectMapper.readerFor(EventMessage.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.eventMessageWriter = objectMapper.writerFor(EventMessage.class);
    }

    /**
     * 将统一事件消息编码为 JSON。
     */
    public String encode(EventMessage message) {
        try {
            return eventMessageWriter.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("事件消息无法序列化为 JSON", exception);
        }
    }

    /**
     * 将 JSON 解码为统一事件消息。
     */
    public EventMessage decode(String messageJson) {
        try {
            return eventMessageReader.readValue(messageJson);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Kafka 消息不是有效的统一事件消息", exception);
        }
    }

    /**
     * 解码一批事件消息并保持原始顺序。
     */
    public List<EventMessage> decodeBatch(List<String> messageJsonList) {
        List<EventMessage> messages = new ArrayList<>(messageJsonList.size());
        for (String messageJson : messageJsonList) {
            messages.add(decode(messageJson));
        }
        return List.copyOf(messages);
    }
}
