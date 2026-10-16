package org.etd.upms.user.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.event.core.codec.EventMessageCodec;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.upms.user.constant.SystemUserEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * {@link UserCreatedEventConsumer} 单元测试。
 */
class UserCreatedEventConsumerTest {

    private UserCreatedEventConsumer consumer;
    private EventMessageCodec eventMessageCodec;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        eventMessageCodec = new EventMessageCodec(objectMapper);
        consumer = new UserCreatedEventConsumer(eventMessageCodec);
    }

    @Test
    @DisplayName("消费用户创建事件时应当正常解析并输出日志")
    void shouldConsumeUserCreatedEventSuccessfully() {
        EventMessage message = new EventMessage(
                "evt_123456",
                SystemUserEventType.USER_CREATED,
                1,
                Instant.now(),
                "etd-business-upms",
                "1001",
                Map.of("traceId", "test-trace-id"),
                objectMapper.createObjectNode().put("userId", 1001L).put("account", "admin")
        );

        String messageJson = eventMessageCodec.encode(message);

        assertDoesNotThrow(() -> consumer.consumeUserCreatedEvent(messageJson));
    }

    @Test
    @DisplayName("消费其他无关事件类型时应当忽略并不报错")
    void shouldIgnoreOtherEventTypes() {
        EventMessage message = new EventMessage(
                "evt_999999",
                "upms.other.event",
                1,
                Instant.now(),
                "etd-business-upms",
                "1001",
                Map.of(),
                objectMapper.createObjectNode().put("data", "test")
        );

        String messageJson = eventMessageCodec.encode(message);

        assertDoesNotThrow(() -> consumer.consumeUserCreatedEvent(messageJson));
    }

    @Test
    @DisplayName("传入非合法 JSON 消息时捕获异常并处理")
    void shouldHandleInvalidJsonFormatGracefully() {
        assertDoesNotThrow(() -> consumer.consumeUserCreatedEvent("invalid json string"));
    }
}
