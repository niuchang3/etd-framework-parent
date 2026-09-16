package org.etd.framework.starter.event.server.delivery.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.event.core.sender.EventMessageSender;
import org.etd.framework.event.core.sender.EventSendResult;
import org.etd.framework.starter.event.server.delivery.codec.EventDeliveryCodec;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryState;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;
import org.etd.framework.starter.event.server.delivery.producer.EventDeliveryStatusPublisher;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 内部投递任务监听器的目标发送与状态顺序测试。
 */
class EventDeliveryTaskListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final EventDeliveryCodec deliveryCodec = new EventDeliveryCodec(objectMapper);

    @Test
    void shouldPublishTargetMessageAndSuccessStatusInOrder() {
        EventMessageSender messageSender = mock(EventMessageSender.class);
        EventDeliveryStatusPublisher statusPublisher = mock(EventDeliveryStatusPublisher.class);
        EventDeliveryTask task = createTask();
        when(messageSender.send(eq(task.targetTopic()), any(EventMessage.class))).thenReturn(
                CompletableFuture.completedFuture(new EventSendResult(
                        task.eventId(), task.targetTopic(), 3, 18L)));

        new EventDeliveryTaskListener(deliveryCodec, messageSender, statusPublisher)
                .consumeDeliveryTask(deliveryCodec.encodeTask(task), 2);

        var ordered = inOrder(statusPublisher, messageSender);
        ordered.verify(statusPublisher).publishDeliveryStatus(argThat(status ->
                status.state() == EventDeliveryState.PUBLISHING && status.attempt() == 2));
        ordered.verify(messageSender).send(eq(task.targetTopic()), argThat(message ->
                message.eventId().equals(task.eventId())));
        ordered.verify(statusPublisher).publishDeliveryStatus(argThat(status ->
                status.state() == EventDeliveryState.SUCCEEDED
                        && status.attempt() == 2
                        && status.partition() == 3
                        && status.offset() == 18L));
    }

    private EventDeliveryTask createTask() {
        EventMessage message = new EventMessage(
                "event-1", "upms.user.created", 1,
                Instant.parse("2026-09-16T01:00:00Z"), "upms", "user-1",
                Map.of(), objectMapper.valueToTree(Map.of("userId", 1L)));
        return new EventDeliveryTask(10L, 20L, 30L,
                message.eventId(), "event-sub-upms-topic", message);
    }
}
