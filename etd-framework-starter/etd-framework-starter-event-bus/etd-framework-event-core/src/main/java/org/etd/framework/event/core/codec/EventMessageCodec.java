package org.etd.framework.event.core.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.event.core.model.EventMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一事件消息 JSON 编解码器，保证发送与转发使用相同的线协议。
 */
public class EventMessageCodec {

    private final ObjectMapper objectMapper;

    public EventMessageCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将统一事件消息编码为 JSON。
     */
    public String encode(EventMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("事件消息无法序列化为 JSON", exception);
        }
    }

    /**
     * 将 JSON 解码为统一事件消息。
     */
    public EventMessage decode(String messageJson) {
        try {
            return objectMapper.readValue(messageJson, EventMessage.class);
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
