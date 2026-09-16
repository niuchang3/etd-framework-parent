package org.etd.framework.starter.event.server.delivery.consumer;

import org.etd.framework.starter.event.server.delivery.codec.EventDeliveryCodec;
import org.etd.framework.starter.event.server.delivery.port.EventDeliveryTaskRegistrar;
import org.springframework.kafka.annotation.KafkaListener;

import static org.etd.framework.starter.event.server.config.EventDeliveryAutoConfiguration.EVENT_DELIVERY_STATUS_LISTENER_CONTAINER_FACTORY;

/**
 * 将内部状态 Topic 的变化异步投影到业务应用维护的 Delivery 表。
 */
public class EventDeliveryStatusListener {

    private final EventDeliveryCodec deliveryCodec;
    private final EventDeliveryTaskRegistrar taskRegistrar;

    public EventDeliveryStatusListener(EventDeliveryCodec deliveryCodec,
                                       EventDeliveryTaskRegistrar taskRegistrar) {
        this.deliveryCodec = deliveryCodec;
        this.taskRegistrar = taskRegistrar;
    }

    /**
     * 幂等应用一条投递状态变化，异常时不提交状态 Topic Offset。
     *
     * @param statusJson 内部状态事件 JSON
     * @throws Exception 状态持久化失败
     */
    @KafkaListener(
            id = "eventDeliveryStatusListener",
            topics = "${etd.event.bus.server.delivery.status-topic:etd.event.delivery.status}",
            groupId = "${etd.event.bus.server.delivery.status-consumer-group:etd-event-delivery-status-projector}",
            containerFactory = EVENT_DELIVERY_STATUS_LISTENER_CONTAINER_FACTORY
    )
    public void consumeDeliveryStatus(String statusJson) throws Exception {
        taskRegistrar.updateDeliveryStatus(deliveryCodec.decodeStatus(statusJson));
    }
}
