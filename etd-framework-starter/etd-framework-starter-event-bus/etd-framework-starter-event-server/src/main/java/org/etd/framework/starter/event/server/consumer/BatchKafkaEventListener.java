package org.etd.framework.starter.event.server.consumer;

import org.springframework.kafka.annotation.KafkaListener;

import java.util.List;

import static org.etd.framework.starter.event.server.config.EventServerAutoConfiguration.EVENT_BATCH_LISTENER_CONTAINER_FACTORY;

/**
 * 批量事件入口监听器，仅在服务端选择批量消费模式时注册。
 */
public class BatchKafkaEventListener {

    private final KafkaEventConsumerAdapter consumerAdapter;

    public BatchKafkaEventListener(KafkaEventConsumerAdapter consumerAdapter) {
        this.consumerAdapter = consumerAdapter;
    }

    /**
     * 接收一次 Kafka 拉取返回的消息集合，并保持每条事件独立的上下文和数据库事务。
     *
     * <p>任意一条处理失败时整批不提交 Offset；已成功落库的事件在批次重投时
     * 由业务持久化实现执行幂等判断。</p>
     *
     * @param messageJsonList 统一事件协议 JSON 列表
     * @throws Exception 任意事件解码或业务处理失败
     */
    @KafkaListener(
            id = "eventBusBatchInboundListener",
            topics = "${etd.event.bus.topic:etd.event.bus}",
            groupId = "${spring.kafka.consumer.group-id:etd-event-server}",
            containerFactory = EVENT_BATCH_LISTENER_CONTAINER_FACTORY
    )
    public void consume(List<String> messageJsonList) throws Exception {
        consumerAdapter.consumeEach(messageJsonList);
    }
}
