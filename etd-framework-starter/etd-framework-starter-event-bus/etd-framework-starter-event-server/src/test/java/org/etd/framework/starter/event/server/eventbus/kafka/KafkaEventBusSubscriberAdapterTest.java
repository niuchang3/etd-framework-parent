package org.etd.framework.starter.event.server.eventbus.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.event.core.codec.EventMessageCodec;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.delivery.producer.EventDeliveryTaskPublisher;
import org.etd.framework.starter.event.server.eventbus.EventBusSubscriberProcessor;
import org.etd.framework.starter.event.server.eventbus.EventBusSubscriberResult;
import org.etd.framework.starter.event.server.eventbus.EventBusSubscriberTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Kafka 统一入口单条、批量消费与监听配置测试。
 */
class KafkaEventBusSubscriberAdapterTest {

    private ObjectMapper objectMapper;
    private KafkaEventBusSubscriberAdapter subscriberAdapter;
    private List<String> traceIdList;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        traceIdList = new ArrayList<>();
        EventBusSubscriberTemplate subscriberTemplate = mock(EventBusSubscriberTemplate.class);
        when(subscriberTemplate.receiveEvent(any())).thenAnswer(invocation -> {
            traceIdList.add(RequestContext.getTraceId());
            return new EventBusSubscriberResult(List.of());
        });
        EventBusSubscriberProcessor processor = new EventBusSubscriberProcessor(
                subscriberTemplate, mock(EventDeliveryTaskPublisher.class));
        subscriberAdapter = new KafkaEventBusSubscriberAdapter(
                new EventMessageCodec(objectMapper), processor);
    }

    @AfterEach
    void cleanRequestContext() {
        RequestContext.clean();
    }

    @Test
    void shouldRestoreAndCleanContextForSingleMessage() throws Exception {
        subscriberAdapter.consumeEvent(toJson(createMessage("event-1", "trace-001")));

        assertThat(traceIdList).containsExactly("trace-001");
        assertThat(RequestContext.getTraceId()).isNull();
    }

    @Test
    void shouldKeepContextsIndependentForBatchMessages() throws Exception {
        subscriberAdapter.consumeEventList(List.of(
                toJson(createMessage("event-1", "trace-001")),
                toJson(createMessage("event-2", "trace-002"))));

        assertThat(traceIdList).containsExactly("trace-001", "trace-002");
        assertThat(RequestContext.getTraceId()).isNull();
    }

    @Test
    void shouldConfigureMutuallySelectableSingleAndBatchListeners() throws Exception {
        KafkaListener singleListener = SingleKafkaEventBusSubscriber.class
                .getMethod("consumeEvent", String.class)
                .getAnnotation(KafkaListener.class);
        KafkaListener batchListener = BatchKafkaEventBusSubscriber.class
                .getMethod("consumeEventList", List.class)
                .getAnnotation(KafkaListener.class);

        assertThat(singleListener.topics()).containsExactly("${etd.event.bus.topic:etd.event.bus}");
        assertThat(singleListener.groupId())
                .isEqualTo("${spring.kafka.consumer.group-id:etd-event-server}");
        assertThat(batchListener.topics()).isEqualTo(singleListener.topics());
        assertThat(batchListener.groupId()).isEqualTo(singleListener.groupId());
        assertThat(batchListener.containerFactory())
                .isEqualTo("eventBatchKafkaListenerContainerFactory");
    }

    private EventMessage createMessage(String eventId, String traceId) {
        return new EventMessage(
                eventId, "upms.user.created", 1, Instant.now(), "upms", eventId,
                Map.of(HeaderConstant.TRACE_ID, traceId),
                objectMapper.valueToTree(Map.of("userId", 1L)));
    }

    private String toJson(EventMessage message) throws Exception {
        return objectMapper.writeValueAsString(message);
    }
}
