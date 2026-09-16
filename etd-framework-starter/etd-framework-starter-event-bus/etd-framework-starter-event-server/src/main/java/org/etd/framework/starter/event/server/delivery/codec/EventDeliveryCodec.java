package org.etd.framework.starter.event.server.delivery.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryStatusEvent;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;

/**
 * Event Server 内部投递任务与状态事件 JSON 编解码器。
 */
public class EventDeliveryCodec {

    private final ObjectReader taskReader;
    private final ObjectWriter taskWriter;
    private final ObjectReader statusReader;
    private final ObjectWriter statusWriter;

    public EventDeliveryCodec(ObjectMapper objectMapper) {
        taskReader = objectMapper.readerFor(EventDeliveryTask.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        taskWriter = objectMapper.writerFor(EventDeliveryTask.class);
        statusReader = objectMapper.readerFor(EventDeliveryStatusEvent.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        statusWriter = objectMapper.writerFor(EventDeliveryStatusEvent.class);
    }

    /** 将投递任务编码为内部 Kafka JSON。 */
    public String encodeTask(EventDeliveryTask task) {
        return write(taskWriter, task, "事件投递任务无法序列化为 JSON");
    }

    /** 将内部 Kafka JSON 解码为投递任务。 */
    public EventDeliveryTask decodeTask(String json) {
        return read(taskReader, json, EventDeliveryTask.class, "Kafka 消息不是有效的事件投递任务");
    }

    /** 将状态变更编码为内部 Kafka JSON。 */
    public String encodeStatus(EventDeliveryStatusEvent statusEvent) {
        return write(statusWriter, statusEvent, "投递状态事件无法序列化为 JSON");
    }

    /** 将内部 Kafka JSON 解码为状态变更。 */
    public EventDeliveryStatusEvent decodeStatus(String json) {
        return read(statusReader, json, EventDeliveryStatusEvent.class, "Kafka 消息不是有效的投递状态事件");
    }

    private String write(ObjectWriter writer, Object value, String errorMessage) {
        try {
            return writer.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(errorMessage, exception);
        }
    }

    private <T> T read(ObjectReader reader, String json, Class<T> type, String errorMessage) {
        try {
            return type.cast(reader.readValue(json));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(errorMessage, exception);
        }
    }
}
