package org.etd.framework.starter.event.client.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.event.core.sender.EventSendResult;
import org.etd.framework.starter.event.client.config.EventClientProperties;
import org.etd.framework.starter.event.client.core.message.DefaultEventMessageFactory;
import org.etd.framework.starter.event.client.core.publisher.DefaultEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * 发送端统一事件消息构建测试。
 */
class EventMessageFactoryTest {

    @AfterEach
    void cleanRequestContext() {
        RequestContext.clean();
    }

    @Test
    void shouldBuildUnifiedEventMessageWithSafeContext() {
        RequestContext.setTraceId("trace-001");
        RequestContext.setTenantCode(1001L);
        RequestContext.setToken("secret-token");
        DefaultEventMessageFactory factory = new DefaultEventMessageFactory(
                () -> "1000001", new ObjectMapper(), "upms");

        EventMessage message = factory.create("upms.user.created", "user-1", Map.of("userId", 1L));

        assertThat(message.eventId()).isEqualTo("1000001");
        assertThat(message.eventVersion()).isEqualTo(EventMessage.INITIAL_VERSION);
        assertThat(message.source()).isEqualTo("upms");
        assertThat(message.payload().get("userId").asLong()).isEqualTo(1L);
        assertThat(message.context()).containsEntry(HeaderConstant.TRACE_ID, "trace-001")
                .containsEntry(HeaderConstant.TENANT_CODE, "1001")
                .doesNotContainKey(HeaderConstant.AUTHORIZATION);
    }

    @Test
    void shouldPublishNewEventWithCapturedContext() {
        RequestContext.setTraceId("trace-002");
        AtomicReference<EventMessage> sentMessage = new AtomicReference<>();
        EventClientProperties properties = new EventClientProperties();
        DefaultEventMessageFactory factory = new DefaultEventMessageFactory(
                () -> "1000002", new ObjectMapper(), "upms");
        DefaultEventPublisher publisher = new DefaultEventPublisher(factory, (destination, message) -> {
            sentMessage.set(message);
            return java.util.concurrent.CompletableFuture.completedFuture(
                    new EventSendResult(message.eventId(), destination));
        }, properties, Runnable::run);

        publisher.publish("upms.user.created", Map.of("userId", 2L));

        assertThat(sentMessage.get().eventId()).isEqualTo("1000002");
        assertThat(sentMessage.get().context()).containsEntry(HeaderConstant.TRACE_ID, "trace-002");
    }

    @Test
    void shouldNotInterruptCallerWhenEventIsInvalid() {
        DefaultEventMessageFactory factory = new DefaultEventMessageFactory(
                () -> "1000003", new ObjectMapper(), "upms");
        DefaultEventPublisher publisher = new DefaultEventPublisher(factory,
                (destination, message) -> null, new EventClientProperties(), Runnable::run);

        assertThatNoException().isThrownBy(() -> publisher.publish(null, null));
    }

}
