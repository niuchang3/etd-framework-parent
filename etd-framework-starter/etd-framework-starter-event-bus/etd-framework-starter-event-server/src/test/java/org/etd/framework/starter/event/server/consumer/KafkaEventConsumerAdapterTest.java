package org.etd.framework.starter.event.server.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.event.core.codec.EventMessageCodec;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.listener.EventTypeDispatcher;
import org.etd.framework.starter.event.server.listener.EventTypeListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 消费端单条消息与批量拉取逐条处理的上下文测试。
 */
class KafkaEventConsumerAdapterTest {

    private ObjectMapper objectMapper;

    private KafkaEventConsumerAdapter consumerAdapter;

    private List<String> traceIds;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        traceIds = new ArrayList<>();
        EventTypeListener listener = createContextRecordingListener();
        consumerAdapter = new KafkaEventConsumerAdapter(
                new EventMessageCodec(objectMapper),
                new EventConsumerInvoker(new EventTypeDispatcher(List.of(listener))));
    }

    @AfterEach
    void cleanRequestContext() {
        RequestContext.clean();
    }

    @Test
    void shouldRestoreAndCleanContextForSingleMessage() throws Exception {
        EventMessage message = createMessage("event-1", "trace-001");

        consumerAdapter.consume(toJson(message));

        assertThat(traceIds).containsExactly("trace-001");
        assertThat(RequestContext.getTraceId()).isNull();
    }

    @Test
    void shouldKeepContextsIndependentWhenBatchIsConsumedOneByOne() throws Exception {
        List<String> messages = List.of(
                toJson(createMessage("event-1", "trace-001")),
                toJson(createMessage("event-2", "trace-002")));

        consumerAdapter.consumeEach(messages);

        assertThat(traceIds).containsExactly("trace-001", "trace-002");
        assertThat(RequestContext.getTraceId()).isNull();
    }

    @Test
    void shouldProvideMutuallySelectableSingleAndBatchListeners() throws Exception {
        KafkaListener singleListener = SingleKafkaEventListener.class
                .getMethod("consume", String.class)
                .getAnnotation(KafkaListener.class);
        KafkaListener batchListener = BatchKafkaEventListener.class
                .getMethod("consume", List.class)
                .getAnnotation(KafkaListener.class);

        assertThat(singleListener).isNotNull();
        assertThat(singleListener.topics()).containsExactly("${etd.event.bus.topic:etd.event.bus}");
        assertThat(singleListener.groupId())
                .isEqualTo("${spring.kafka.consumer.group-id:etd-event-server}");
        assertThat(batchListener).isNotNull();
        assertThat(batchListener.topics()).isEqualTo(singleListener.topics());
        assertThat(batchListener.groupId()).isEqualTo(singleListener.groupId());
        assertThat(batchListener.containerFactory())
                .isEqualTo("eventBatchKafkaListenerContainerFactory");
    }

    private EventMessage createMessage(String eventId, String traceId) {
        return new EventMessage(
                eventId, "upms.user.created", 1, Instant.now(), "upms", eventId,
                Map.of(HeaderConstant.TRACE_ID, traceId), objectMapper.valueToTree(Map.of("userId", 1L)));
    }

    private String toJson(EventMessage message) throws Exception {
        return objectMapper.writeValueAsString(message);
    }

    private EventTypeListener createContextRecordingListener() {
        return new EventTypeListener() {
            @Override
            public Set<String> eventTypes() {
                return Set.of("upms.user.created");
            }

            @Override
            public void onEvent(EventMessage message) {
                traceIds.add(RequestContext.getTraceId());
            }
        };
    }
}
