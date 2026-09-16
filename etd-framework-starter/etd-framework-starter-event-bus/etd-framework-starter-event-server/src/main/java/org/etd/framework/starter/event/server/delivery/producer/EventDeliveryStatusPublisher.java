package org.etd.framework.starter.event.server.delivery.producer;

import org.etd.framework.starter.event.server.delivery.codec.EventDeliveryCodec;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryStatusEvent;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * 将投递状态变化写入内部状态 Topic。
 */
public class EventDeliveryStatusPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final EventDeliveryCodec deliveryCodec;
    private final String statusTopic;

    public EventDeliveryStatusPublisher(KafkaTemplate<Object, Object> kafkaTemplate,
                                        EventDeliveryCodec deliveryCodec,
                                        String statusTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.deliveryCodec = deliveryCodec;
        this.statusTopic = statusTopic;
    }

    /**
     * 在当前 Kafka 事务中发布状态，并等待 Broker 返回结果。
     *
     * @param statusEvent 状态事件
     */
    public void publishDeliveryStatus(EventDeliveryStatusEvent statusEvent) {
        kafkaTemplate.send(statusTopic, statusEvent.deliveryId().toString(),
                deliveryCodec.encodeStatus(statusEvent)).join();
    }

    /**
     * 在原消费事务回滚后开启独立 Kafka 事务记录等待重试状态。
     *
     * @param statusEvent 状态事件
     */
    public void publishDeliveryStatusInNewTransaction(EventDeliveryStatusEvent statusEvent) {
        kafkaTemplate.executeInTransaction(operations -> {
            operations.send(statusTopic, statusEvent.deliveryId().toString(),
                    deliveryCodec.encodeStatus(statusEvent)).join();
            return null;
        });
    }
}
