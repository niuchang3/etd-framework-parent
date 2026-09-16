package org.etd.framework.starter.event.server.eventbus.kafka;

import org.springframework.kafka.annotation.KafkaListener;

import java.util.List;

import static org.etd.framework.starter.event.server.config.EventServerAutoConfiguration.EVENT_BATCH_LISTENER_CONTAINER_FACTORY;

/**
 * Kafka 统一入口 Topic 批量监听器，仅在批量消费模式下注册。
 */
public class BatchKafkaEventBusSubscriber {

    private final KafkaEventBusSubscriberAdapter subscriberAdapter;

    public BatchKafkaEventBusSubscriber(KafkaEventBusSubscriberAdapter subscriberAdapter) {
        this.subscriberAdapter = subscriberAdapter;
    }

    /**
     * 接收一次 Kafka 拉取的消息集合，并逐条建立上下文和业务事务。
     *
     * <p>任意一条处理失败时整批不提交 Offset；已落库事件在批次重投时
     * 由业务入口实现执行幂等校验。</p>
     *
     * @param messageJsonList 统一事件协议 JSON 列表
     * @throws Exception 任意事件解码或入口处理失败
     */
    @KafkaListener(
            id = "eventBusBatchSubscriber",
            topics = "${etd.event.bus.topic:etd.event.bus}",
            groupId = "${spring.kafka.consumer.group-id:etd-event-server}",
            containerFactory = EVENT_BATCH_LISTENER_CONTAINER_FACTORY
    )
    public void consumeEventList(List<String> messageJsonList) throws Exception {
        subscriberAdapter.consumeEventList(messageJsonList);
    }
}
