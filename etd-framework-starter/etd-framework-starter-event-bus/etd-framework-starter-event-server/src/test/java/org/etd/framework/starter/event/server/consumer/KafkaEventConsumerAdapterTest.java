package org.etd.framework.starter.event.server.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.event.core.codec.EventMessageCodec;
import org.etd.framework.event.core.model.EventMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 消费端单条与批量消息上下文测试。
 */
class KafkaEventConsumerAdapterTest {

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
                eventId, "upms.user.created", 1, Instant.now(), "upms", eventId,
                Map.of(HeaderConstant.TRACE_ID, traceId), objectMapper.valueToTree(Map.of("userId", 1L)));
    }

    private String toJson(EventMessage message) throws Exception {
        return objectMapper.writeValueAsString(message);
    }
}
