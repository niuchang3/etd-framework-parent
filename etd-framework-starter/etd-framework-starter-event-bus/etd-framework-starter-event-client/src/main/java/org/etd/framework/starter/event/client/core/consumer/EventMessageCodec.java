package org.etd.framework.starter.event.client.core.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.starter.event.client.core.model.EventMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Kafka JSON 数据与统一事件消息之间的编解码器。
 */
public class EventMessageCodec {

    private final ObjectMapper objectMapper;

    public EventMessageCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析一条事件消息。
     *
     * @param messageJson Kafka JSON 消息
     * @return 统一事件消息
     */
    public EventMessage decode(String messageJson) {
        try {
            return objectMapper.readValue(messageJson, EventMessage.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Kafka 消息不是有效的统一事件消息", exception);
        }
    }

    /**
     * 解析一批事件消息并保持 Kafka 原始顺序。
     *
     * @param messageJsonList Kafka JSON 消息列表
     * @return 统一事件消息列表
     */
    public List<EventMessage> decodeBatch(List<String> messageJsonList) {
        List<EventMessage> messages = new ArrayList<>(messageJsonList.size());
        for (String messageJson : messageJsonList) {
            messages.add(decode(messageJson));
        }
        return List.copyOf(messages);
    }
}
