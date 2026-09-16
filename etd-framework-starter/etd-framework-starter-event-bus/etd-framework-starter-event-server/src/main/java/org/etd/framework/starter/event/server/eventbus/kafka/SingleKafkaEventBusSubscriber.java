package org.etd.framework.starter.event.server.eventbus.kafka;

import org.springframework.kafka.annotation.KafkaListener;

/**
 * Kafka 统一入口 Topic 单条监听器，仅在单条消费模式下注册。
 */
public class SingleKafkaEventBusSubscriber {

    private final KafkaEventBusSubscriberAdapter subscriberAdapter;

    public SingleKafkaEventBusSubscriber(KafkaEventBusSubscriberAdapter subscriberAdapter) {
        this.subscriberAdapter = subscriberAdapter;
    }

    /**
     * 接收统一入口 Topic 的一条消息，异常继续交给监听容器处理。
     *
     * @param messageJson 统一事件协议 JSON
     * @throws Exception 事件解码或入口处理失败
     */
    @KafkaListener(
            id = "eventBusSingleSubscriber",
            topics = "${etd.event.bus.topic:etd.event.bus}",
            groupId = "${spring.kafka.consumer.group-id:etd-event-server}"
    )
    public void consumeEvent(String messageJson) throws Exception {
        subscriberAdapter.consumeEvent(messageJson);
    }
}
