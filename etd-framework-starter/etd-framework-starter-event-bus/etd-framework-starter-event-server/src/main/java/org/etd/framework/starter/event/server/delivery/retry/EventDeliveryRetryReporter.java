package org.etd.framework.starter.event.server.delivery.retry;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.etd.framework.starter.event.server.delivery.codec.EventDeliveryCodec;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryState;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryStatusEvent;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;
import org.etd.framework.starter.event.server.delivery.producer.EventDeliveryStatusPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.RetryListener;

import java.time.Instant;

/**
 * 在投递事务回滚后，以独立 Kafka 事务记录等待重试状态。
 */
public class EventDeliveryRetryReporter implements RetryListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDeliveryRetryReporter.class);
    private static final int MAX_ERROR_LENGTH = 2000;

    private final EventDeliveryCodec deliveryCodec;
    private final EventDeliveryStatusPublisher statusPublisher;
    private final EventDeliveryRetryPolicy retryPolicy;
    private final int maxAttempts;

    public EventDeliveryRetryReporter(EventDeliveryCodec deliveryCodec,
                                      EventDeliveryStatusPublisher statusPublisher,
                                      EventDeliveryRetryPolicy retryPolicy,
                                      int maxAttempts) {
        this.deliveryCodec = deliveryCodec;
        this.statusPublisher = statusPublisher;
        this.retryPolicy = retryPolicy;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public void failedDelivery(ConsumerRecord<?, ?> record, Exception exception, int deliveryAttempt) {
        if (deliveryAttempt >= maxAttempts) {
            return;
        }
        try {
            EventDeliveryTask task = deliveryCodec.decodeTask(record.value().toString());
            statusPublisher.publishDeliveryStatusInNewTransaction(
                    createRetryStatus(task, exception, deliveryAttempt));
        } catch (RuntimeException reportException) {
            LOGGER.error("投递等待重试状态发布失败 topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset(), reportException);
        }
    }

    private EventDeliveryStatusEvent createRetryStatus(EventDeliveryTask task,
                                                       Exception exception,
                                                       int deliveryAttempt) {
        Instant nextRetryAt = Instant.now().plus(
                retryPolicy.calculateRetryBackoff(deliveryAttempt));
        return new EventDeliveryStatusEvent(
                task.deliveryId(), task.eventId(), EventDeliveryState.RETRY_WAITING,
                deliveryAttempt, task.targetTopic(), null, null, nextRetryAt, null,
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
