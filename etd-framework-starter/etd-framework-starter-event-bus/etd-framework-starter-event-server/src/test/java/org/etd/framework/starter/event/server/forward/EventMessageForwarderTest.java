package org.etd.framework.starter.event.server.forward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.event.core.codec.EventMessageCodec;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.event.core.sender.EventMessageSender;
import org.etd.framework.event.core.sender.EventSendResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 服务端透明转发测试。
 */
class EventMessageForwarderTest {

    @Test
    void shouldForwardUnifiedMessageAndContextWithoutRebuilding() {
        AtomicReference<EventMessage> forwardedMessage = new AtomicReference<>();
        EventMessageCodec codec = new EventMessageCodec(
                new ObjectMapper().registerModule(new JavaTimeModule()));
        EventMessageSender sender = (destination, message) -> {
            // 模拟 Kafka JSON 线协议往返，验证转发后仍是同一份消息内容。
            forwardedMessage.set(codec.decode(codec.encode(message)));
            return CompletableFuture.completedFuture(new EventSendResult(message.eventId(), destination));
        };
        EventMessage message = createMessage();

        new EventMessageForwarder(sender, Runnable::run)
                .forward("etd.event.delivery.upms", message);

        EventMessage forwarded = forwardedMessage.get();
        assertThat(forwarded.eventId()).isEqualTo(message.eventId());
        assertThat(forwarded.eventType()).isEqualTo(message.eventType());
        assertThat(forwarded.eventVersion()).isEqualTo(message.eventVersion());
        assertThat(forwarded.occurredAt()).isEqualTo(message.occurredAt());
        assertThat(forwarded.source()).isEqualTo(message.source());
        assertThat(forwarded.partitionKey()).isEqualTo(message.partitionKey());
        assertThat(forwarded.payload().get("userId").asLong()).isEqualTo(1L);
        assertThat(forwarded.context())
                .containsEntry(HeaderConstant.TRACE_ID, "trace-001");
    }

    private EventMessage createMessage() {
        return new EventMessage(
                "event-1", "upms.user.created", 1, Instant.now(), "upms", "user-1",
                Map.of(HeaderConstant.TRACE_ID, "trace-001"),
                new ObjectMapper().valueToTree(Map.of("userId", 1L)));
    }
}
