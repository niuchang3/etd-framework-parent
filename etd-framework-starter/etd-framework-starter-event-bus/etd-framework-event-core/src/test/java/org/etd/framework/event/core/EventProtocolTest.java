package org.etd.framework.event.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.etd.framework.event.core.codec.EventMessageCodec;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.event.core.sender.EventSendResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 事件总线协议严格校验测试。
 */
class EventProtocolTest {

    @Test
    void shouldRejectMessageWithoutContext() {
        assertThatThrownBy(() -> new EventMessage(
                "event-1", "upms.user.created", 1, Instant.now(), "upms", "user-1",
                null, new ObjectMapper().valueToTree("payload")))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("事件上下文不能为空");
    }

    @Test
    void shouldRejectBlankDestination() {
        assertThatThrownBy(() -> new EventSendResult("event-1", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("事件发送目的地不能为空");
    }

    /**
     * 未知字段不属于当前消费端关心的协议内容，应当被忽略。
     */
    @Test
    void shouldIgnoreUnknownProtocolField() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        EventMessageCodec codec = new EventMessageCodec(objectMapper);
        String messageJson = """
                {
                  "eventId":"event-1",
                  "eventType":"upms.user.created",
                  "eventVersion":1,
                  "occurredAt":"2026-09-14T06:00:00Z",
                  "source":"upms",
                  "partitionKey":"user-1",
                  "context":{},
                  "payload":{},
                  "futureField":"future-value"
                }
                """;

        EventMessage message = codec.decode(messageJson);

        assertThat(message.eventId()).isEqualTo("event-1");
    }

}
