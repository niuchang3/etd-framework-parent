package org.etd.framework.starter.event.server.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.etd.framework.event.core.codec.EventMessageCodec;
import org.etd.framework.event.core.sender.EventMessageSender;
import org.etd.framework.starter.event.client.config.EventBusProperties;
import org.etd.framework.starter.event.client.config.EventClientAutoConfiguration;
import org.etd.framework.starter.event.server.consumer.EventConsumerInvoker;
import org.etd.framework.starter.event.server.consumer.KafkaEventConsumerAdapter;
import org.etd.framework.starter.event.server.forward.EventMessageForwarder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.util.Assert;

import static org.etd.framework.starter.event.client.config.EventClientAutoConfiguration.EVENT_BUS_TASK_EXECUTOR;

/**
 * 事件总线消费端 Kafka 自动配置。
 */
@AutoConfiguration
@AutoConfigureAfter(EventClientAutoConfiguration.class)
@ConditionalOnClass(Consumer.class)
@EnableConfigurationProperties(EventServerProperties.class)
public class EventServerAutoConfiguration {

    /**
     * 由服务端声明统一入口 Topic。KafkaAdmin 会以幂等方式创建不存在的 Topic。
     */
    @Bean(name = "eventBusTopic")
    @ConditionalOnProperty(prefix = "etd.event.bus.server", name = "create-topic",
            havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "eventBusTopic")
    public KafkaAdmin.NewTopics eventBusTopic(EventBusProperties busProperties,
                                              EventServerProperties serverProperties) {
        validateTopicProperties(busProperties, serverProperties);
        NewTopic topic = TopicBuilder.name(busProperties.getTopic())
                .partitions(serverProperties.getPartitions())
                .replicas(serverProperties.getReplicas())
                .build();
        return new KafkaAdmin.NewTopics(topic);
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
    @ConditionalOnMissingBean(EventMessageForwarder.class)
    public EventMessageForwarder eventMessageForwarder(EventMessageSender eventMessageSender,
                                                       @Qualifier(EVENT_BUS_TASK_EXECUTOR)
                                                       TaskExecutor taskExecutor) {
        return new EventMessageForwarder(eventMessageSender, taskExecutor);
    }

    private void validateTopicProperties(EventBusProperties busProperties,
                                         EventServerProperties serverProperties) {
        Assert.hasText(busProperties.getTopic(), "事件总线入口 Topic 不能为空");
        Assert.isTrue(serverProperties.getPartitions() > 0, "事件总线入口 Topic 分区数必须大于 0");
        Assert.isTrue(serverProperties.getReplicas() > 0, "事件总线入口 Topic 副本数必须大于 0");
    }
}
