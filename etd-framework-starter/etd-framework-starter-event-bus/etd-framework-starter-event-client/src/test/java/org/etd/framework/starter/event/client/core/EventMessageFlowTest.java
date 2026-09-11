package org.etd.framework.starter.event.client.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.event.client.core.consumer.EventConsumerInvoker;
import org.etd.framework.starter.event.client.core.consumer.EventMessageCodec;
import org.etd.framework.starter.event.client.core.consumer.KafkaEventConsumerAdapter;
import org.etd.framework.starter.event.client.core.message.DefaultEventMessageFactory;
import org.etd.framework.starter.event.client.core.model.EventMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一事件模型及单条、批量消费流程测试。
 */
class EventMessageFlowTest {

    private ObjectMapper objectMapper;

    private KafkaEventConsumerAdapter consumerAdapter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumerAdapter = new KafkaEventConsumerAdapter(
                new EventMessageCodec(objectMapper), new EventConsumerInvoker());
    }

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
                () -> "1000001", objectMapper, "upms");

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
    void shouldRestoreAndCleanContextForSingleMessage() throws Exception {
        EventMessage message = createMessage("event-1", "trace-001");

        consumerAdapter.consume(toJson(message), consumed ->
                assertThat(RequestContext.getTraceId()).isEqualTo("trace-001"));

        assertThat(RequestContext.getTraceId()).isNull();
    }

    @Test
    void shouldKeepContextsIndependentWhenBatchIsConsumedOneByOne() throws Exception {
        List<String> traceIds = new ArrayList<>();
        List<String> messages = List.of(
                toJson(createMessage("event-1", "trace-001")),
                toJson(createMessage("event-2", "trace-002")));

        consumerAdapter.consumeEach(messages, consumed -> traceIds.add(RequestContext.getTraceId()));

        assertThat(traceIds).containsExactly("trace-001", "trace-002");
        assertThat(RequestContext.getTraceId()).isNull();
    }

    @Test
    void shouldNotCreateFalseSharedContextForBatchHandler() throws Exception {
        List<String> messages = List.of(
                toJson(createMessage("event-1", "trace-001")),
                toJson(createMessage("event-2", "trace-002")));

        consumerAdapter.consumeBatch(messages, consumed -> {
            assertThat(RequestContext.getTraceId()).isNull();
            assertThat(consumed).extracting(item -> item.context().get(HeaderConstant.TRACE_ID))
                    .containsExactly("trace-001", "trace-002");
        });
    }

    private EventMessage createMessage(String eventId, String traceId) {
        return new EventMessage(
                eventId, "upms.user.created", 1, java.time.Instant.now(), "upms", eventId,
                Map.of(HeaderConstant.TRACE_ID, traceId), objectMapper.valueToTree(Map.of("userId", 1L)));
    }

    private String toJson(EventMessage message) throws Exception {
        return objectMapper.writeValueAsString(message);
    }
}
