package org.etd.framework.starter.event.server.delivery.consumer;

import org.etd.framework.event.core.sender.EventMessageSender;
import org.etd.framework.event.core.sender.EventSendResult;
import org.etd.framework.starter.event.server.delivery.codec.EventDeliveryCodec;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryState;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryStatusEvent;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;
import org.etd.framework.starter.event.server.delivery.producer.EventDeliveryStatusPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

import java.time.Instant;

import static org.etd.framework.starter.event.server.config.EventDeliveryAutoConfiguration.EVENT_DELIVERY_TASK_LISTENER_CONTAINER_FACTORY;

/**
 * 消费内部投递任务，并在一个 Kafka 消费事务中发送目标消息和成功状态。
 */
public class EventDeliveryTaskListener {

    private final EventDeliveryCodec deliveryCodec;
    private final EventMessageSender messageSender;
    private final EventDeliveryStatusPublisher statusPublisher;

    public EventDeliveryTaskListener(EventDeliveryCodec deliveryCodec,
                                     EventMessageSender messageSender,
                                     EventDeliveryStatusPublisher statusPublisher) {
        this.deliveryCodec = deliveryCodec;
        this.messageSender = messageSender;
        this.statusPublisher = statusPublisher;
    }

    /**
     * 将一条投递任务原样发送到订阅 Topic；异常交给事务回滚处理器重试。
     *
     * @param taskJson 内部投递任务 JSON
     * @param deliveryAttempt 当前容器投递次数，首次消费时可能不存在
     */
    @KafkaListener(
            id = "eventDeliveryTaskListener",
            topics = "${etd.event.bus.server.delivery.task-topic:etd.event.delivery.task}",
            groupId = "${etd.event.bus.server.delivery.task-consumer-group:etd-event-delivery-dispatcher}",
            containerFactory = EVENT_DELIVERY_TASK_LISTENER_CONTAINER_FACTORY
    )
    public void consumeDeliveryTask(
            String taskJson,
            @Header(name = KafkaHeaders.DELIVERY_ATTEMPT, required = false) Integer deliveryAttempt) {
        EventDeliveryTask task = deliveryCodec.decodeTask(taskJson);
        int attempt = deliveryAttempt == null ? 1 : deliveryAttempt;
        statusPublisher.publishDeliveryStatus(createPublishingStatus(task, attempt));
        EventSendResult result = messageSender.send(task.targetTopic(), task.message()).join();
        statusPublisher.publishDeliveryStatus(createSucceededStatus(task, attempt, result));
    }

    private EventDeliveryStatusEvent createPublishingStatus(EventDeliveryTask task, int attempt) {
        return new EventDeliveryStatusEvent(
                task.deliveryId(), task.eventId(), EventDeliveryState.PUBLISHING,
                attempt, task.targetTopic(), null, null, null, null, null);
    }

    private EventDeliveryStatusEvent createSucceededStatus(EventDeliveryTask task,
                                                            int attempt,
                                                            EventSendResult result) {
        return new EventDeliveryStatusEvent(
                task.deliveryId(), task.eventId(), EventDeliveryState.SUCCEEDED,
                attempt, task.targetTopic(), result.partition(), result.offset(),
                null, Instant.now(), null);
    }
}
