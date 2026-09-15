package org.etd.framework.starter.event.server.consumer;

import org.springframework.kafka.annotation.KafkaListener;

/**
 * 单条事件入口监听器，仅在服务端选择单条消费模式时注册。
 */
public class SingleKafkaEventListener {

    private final KafkaEventConsumerAdapter consumerAdapter;

    public SingleKafkaEventListener(KafkaEventConsumerAdapter consumerAdapter) {
        this.consumerAdapter = consumerAdapter;
    }

    /**
     * 逐条接收统一入口 Topic 的事件，异常继续交给监听容器处理。
     *
     * @param messageJson 统一事件协议 JSON
     * @throws Exception 事件解码或业务处理失败
     */
    @KafkaListener(
            id = "eventBusSingleInboundListener",
            topics = "${etd.event.bus.topic:etd.event.bus}",
            groupId = "${spring.kafka.consumer.group-id:etd-event-server}"
    )
    public void consume(String messageJson) throws Exception {
        consumerAdapter.consume(messageJson);
    }
}
