package org.etd.framework.starter.event.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.etd.framework.event.core.codec.EventMessageCodec;
import org.etd.framework.event.core.sender.EventMessageSender;
import org.etd.framework.starter.event.client.config.EventClientAutoConfiguration;
import org.etd.framework.starter.event.server.consumer.EventConsumerInvoker;
import org.etd.framework.starter.event.server.consumer.KafkaEventConsumerAdapter;
import org.etd.framework.starter.event.server.forward.EventMessageForwarder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;

import static org.etd.framework.starter.event.client.config.EventClientAutoConfiguration.EVENT_BUS_TASK_EXECUTOR;

/**
 * 事件总线消费端 Kafka 自动配置。
 */
@AutoConfiguration
@AutoConfigureAfter(EventClientAutoConfiguration.class)
@ConditionalOnClass(Consumer.class)
public class EventServerAutoConfiguration {

    /**
     * 创建统一事件消息 JSON 解码器。
     */
    @Bean
    @ConditionalOnMissingBean(EventMessageCodec.class)
    public EventMessageCodec eventMessageCodec(ObjectMapper objectMapper) {
        return new EventMessageCodec(objectMapper);
    }

    /**
     * 创建单条和批量消费共用的上下文调用器。
     */
    @Bean
    @ConditionalOnMissingBean(EventConsumerInvoker.class)
    public EventConsumerInvoker eventConsumerInvoker() {
        return new EventConsumerInvoker();
    }

    /**
     * 创建 Kafka 单条和批量消费入口适配器。
     */
    @Bean
    @ConditionalOnMissingBean(KafkaEventConsumerAdapter.class)
    public KafkaEventConsumerAdapter kafkaEventConsumerAdapter(EventMessageCodec eventMessageCodec,
                                                               EventConsumerInvoker eventConsumerInvoker) {
        return new KafkaEventConsumerAdapter(eventMessageCodec, eventConsumerInvoker);
    }

    /**
     * 创建复用发送端能力的透明事件转发器。
     */
    @Bean
    @ConditionalOnBean(EventMessageSender.class)
    @ConditionalOnMissingBean(EventMessageForwarder.class)
    public EventMessageForwarder eventMessageForwarder(EventMessageSender eventMessageSender,
                                                       @Qualifier(EVENT_BUS_TASK_EXECUTOR)
                                                       TaskExecutor taskExecutor) {
        return new EventMessageForwarder(eventMessageSender, taskExecutor);
    }
}
