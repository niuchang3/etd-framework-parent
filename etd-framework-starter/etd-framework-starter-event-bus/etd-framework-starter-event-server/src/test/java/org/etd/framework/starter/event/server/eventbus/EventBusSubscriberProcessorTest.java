package org.etd.framework.starter.event.server.eventbus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;
import org.etd.framework.starter.event.server.delivery.producer.EventDeliveryTaskPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 统一入口事件处理、上下文边界和异常传播测试。
 */
class EventBusSubscriberProcessorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void cleanRequestContext() {
        RequestContext.clean();
    }

    @Test
    void shouldReceiveEventAndSkipPublishingWhenNoTaskCreated() throws Exception {
        String[] receivedTraceId = new String[1];
        EventDeliveryTaskPublisher taskPublisher = mock(EventDeliveryTaskPublisher.class);
        EventBusSubscriberTemplate subscriberTemplate = mock(EventBusSubscriberTemplate.class);
        when(subscriberTemplate.receiveEvent(any()))
                .thenAnswer(invocation -> {
                    receivedTraceId[0] = RequestContext.getTraceId();
                    return new EventBusSubscriberResult(List.of());
                });
        EventBusSubscriberProcessor processor =
                new EventBusSubscriberProcessor(subscriberTemplate, taskPublisher);

        processor.processEvent(createMessage());

        assertThat(receivedTraceId[0]).isEqualTo("trace-001");
        assertThat(RequestContext.getTraceId()).isNull();
        verifyNoInteractions(taskPublisher);
    }

    @Test
    void shouldPropagateBusinessFailureAndCleanContext() {
        EventBusSubscriberTemplate subscriberTemplate = mock(EventBusSubscriberTemplate.class);
        when(subscriberTemplate.receiveEvent(any()))
                .thenThrow(new IllegalStateException("数据库不可用"));
        EventBusSubscriberProcessor processor = new EventBusSubscriberProcessor(
                subscriberTemplate, mock(EventDeliveryTaskPublisher.class));

        assertThatThrownBy(() -> processor.processEvent(createMessage()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("数据库不可用");
        assertThat(RequestContext.getTraceId()).isNull();
    }

    @Test
    void shouldPublishCreatedDeliveryTasks() throws Exception {
        EventDeliveryTask task = mock(EventDeliveryTask.class);
        EventDeliveryTaskPublisher taskPublisher = mock(EventDeliveryTaskPublisher.class);
        EventBusSubscriberTemplate subscriberTemplate = mock(EventBusSubscriberTemplate.class);
        when(subscriberTemplate.receiveEvent(any()))
                .thenReturn(new EventBusSubscriberResult(List.of(task)));
        EventBusSubscriberProcessor processor =
                new EventBusSubscriberProcessor(subscriberTemplate, taskPublisher);

        processor.processEvent(createMessage());

        verify(taskPublisher).publishDeliveryTaskList(List.of(task));
    }

    private EventMessage createMessage() {
        return new EventMessage(
                "event-1", "upms.user.created", 1,
                Instant.parse("2026-09-15T01:00:00Z"), "upms", "user-1",
                Map.of(HeaderConstant.TRACE_ID, "trace-001"),
                objectMapper.valueToTree(Map.of("userId", 1L)));
    }
}
