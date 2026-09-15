package org.etd.framework.starter.event.server.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.etd.framework.event.core.codec.EventMessageCodec;
import org.etd.framework.event.core.sender.EventMessageSender;
import org.etd.framework.starter.event.client.config.EventBusProperties;
import org.etd.framework.starter.event.client.config.EventClientAutoConfiguration;
import org.etd.framework.starter.event.server.consumer.BatchKafkaEventListener;
import org.etd.framework.starter.event.server.consumer.EventConsumerInvoker;
import org.etd.framework.starter.event.server.consumer.KafkaEventConsumerAdapter;
import org.etd.framework.starter.event.server.consumer.SingleKafkaEventListener;
import org.etd.framework.starter.event.server.forward.EventMessageForwarder;
import org.etd.framework.starter.event.server.listener.EventTypeDispatcher;
import org.etd.framework.starter.event.server.listener.EventTypeListener;
import org.etd.framework.starter.event.server.persistence.EventMessagePersistence;
import org.etd.framework.starter.event.server.persistence.EventPersistenceListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.ContainerProperties;
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
     * Spring Kafka 监听容器配置前缀。
     */
    private static final String KAFKA_LISTENER_PROPERTY_PREFIX = "spring.kafka.listener";

    /**
     * Spring Kafka 单条监听类型配置值。
     */
    private static final String KAFKA_LISTENER_TYPE_SINGLE = "single";

    /**
     * Spring Kafka 批量监听类型配置值。
     */
    private static final String KAFKA_LISTENER_TYPE_BATCH = "batch";

    /**
     * 批量事件监听器专用容器工厂 Bean 名称。
     */
    public static final String EVENT_BATCH_LISTENER_CONTAINER_FACTORY =
            "eventBatchKafkaListenerContainerFactory";

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
     * 将业务应用提供的持久化端口注册为最高优先级的全局事件监听器。
     *
     * @param eventMessagePersistence 业务持久化实现
     * @return 持久化事件监听器
     */
    @Bean
    @ConditionalOnMissingBean(EventPersistenceListener.class)
    public EventPersistenceListener eventPersistenceListener(EventMessagePersistence eventMessagePersistence) {
        return new EventPersistenceListener(eventMessagePersistence);
    }

    /**
     * 收集全部事件类型监听器，并按 Spring 顺序规则创建事件分发器。
     */
    @Bean
    @ConditionalOnMissingBean(EventTypeDispatcher.class)
    public EventTypeDispatcher eventTypeDispatcher(ObjectProvider<EventTypeListener> listenerProvider) {
        return new EventTypeDispatcher(listenerProvider.orderedStream().toList());
    }

    /**
     * 创建单条和批量拉取逐条消费共用的上下文调用器。
     */
    @Bean
    @ConditionalOnMissingBean(EventConsumerInvoker.class)
    public EventConsumerInvoker eventConsumerInvoker(EventTypeDispatcher eventTypeDispatcher) {
        return new EventConsumerInvoker(eventTypeDispatcher);
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
     * 单条模式下注册逐条事件监听入口。
     *
     * @param consumerAdapter Kafka 消费适配器
     * @return 单条事件监听器
     */
    @Bean
    @ConditionalOnProperty(prefix = KAFKA_LISTENER_PROPERTY_PREFIX, name = "type",
            havingValue = KAFKA_LISTENER_TYPE_SINGLE, matchIfMissing = true)
    public SingleKafkaEventListener singleKafkaEventListener(KafkaEventConsumerAdapter consumerAdapter) {
        return new SingleKafkaEventListener(consumerAdapter);
    }

    /**
     * 创建批量监听专用容器工厂，并覆盖公共配置中的单条 ACK 模式。
     *
     * @param configurer Spring Boot Kafka 监听容器配置器
     * @param consumerFactory Kafka Consumer 工厂
     * @return 批量监听容器工厂
     */
    @Bean(name = EVENT_BATCH_LISTENER_CONTAINER_FACTORY)
    @ConditionalOnProperty(prefix = KAFKA_LISTENER_PROPERTY_PREFIX, name = "type",
            havingValue = KAFKA_LISTENER_TYPE_BATCH)
    public ConcurrentKafkaListenerContainerFactory<Object, Object> eventBatchKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory);
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        return factory;
    }

    /**
     * 批量模式下注册集合事件监听入口。
     *
     * @param consumerAdapter Kafka 消费适配器
     * @return 批量事件监听器
     */
    @Bean
    @ConditionalOnProperty(prefix = KAFKA_LISTENER_PROPERTY_PREFIX, name = "type",
            havingValue = KAFKA_LISTENER_TYPE_BATCH)
    public BatchKafkaEventListener batchKafkaEventListener(KafkaEventConsumerAdapter consumerAdapter) {
        return new BatchKafkaEventListener(consumerAdapter);
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
