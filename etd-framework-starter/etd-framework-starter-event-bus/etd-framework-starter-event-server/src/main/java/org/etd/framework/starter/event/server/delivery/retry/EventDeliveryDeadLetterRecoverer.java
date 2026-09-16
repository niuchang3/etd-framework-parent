package org.etd.framework.starter.event.server.delivery.retry;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.etd.framework.starter.event.server.delivery.codec.EventDeliveryCodec;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryState;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryStatusEvent;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;
import org.etd.framework.starter.event.server.delivery.producer.EventDeliveryStatusPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

/**
 * 在事务性恢复阶段同时写入死信 Topic 和最终 DEAD 状态。
 */
public class EventDeliveryDeadLetterRecoverer {

    private static final int MAX_ERROR_LENGTH = 2000;

    private final DeadLetterPublishingRecoverer deadLetterPublisher;
    private final EventDeliveryCodec deliveryCodec;
    private final EventDeliveryStatusPublisher statusPublisher;
    private final int maxAttempts;

    public EventDeliveryDeadLetterRecoverer(KafkaTemplate<Object, Object> kafkaTemplate,
                                            EventDeliveryCodec deliveryCodec,
                                            EventDeliveryStatusPublisher statusPublisher,
                                            String deadLetterTopic,
                                            int maxAttempts) {
        deadLetterPublisher = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(deadLetterTopic, record.partition()));
        deadLetterPublisher.setFailIfSendResultIsError(true);
        this.deliveryCodec = deliveryCodec;
        this.statusPublisher = statusPublisher;
        this.maxAttempts = maxAttempts;
    }

    /**
     * 将重试耗尽的原始任务和最终状态写入同一个 Kafka 恢复事务。
     *
     * @param record 投递任务记录
     * @param exception 最后一次发送异常
     */
    public void recover(ConsumerRecord<?, ?> record, Exception exception) {
        deadLetterPublisher.accept(record, exception);
        EventDeliveryTask task = deliveryCodec.decodeTask(record.value().toString());
        statusPublisher.publishDeliveryStatus(createDeadStatus(task, exception));
    }

    private EventDeliveryStatusEvent createDeadStatus(EventDeliveryTask task, Exception exception) {
        return new EventDeliveryStatusEvent(
                task.deliveryId(), task.eventId(), EventDeliveryState.DEAD,
                maxAttempts, task.targetTopic(), null, null, null, null,
                summarizeException(exception));
    }

    private String summarizeException(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getClass().getSimpleName() + ": " + current.getMessage();
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
