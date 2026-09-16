package org.etd.framework.starter.event.server.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.etd.framework.event.core.codec.EventMessageCodec;
import org.etd.framework.starter.event.client.config.EventBusProperties;
import org.etd.framework.starter.event.client.config.EventClientAutoConfiguration;
import org.etd.framework.starter.event.server.delivery.producer.EventDeliveryTaskPublisher;
import org.etd.framework.starter.event.server.delivery.port.EventDeliveryTaskRegistrar;
import org.etd.framework.starter.event.server.eventbus.EventBusSubscriberProcessor;
import org.etd.framework.starter.event.server.eventbus.EventBusSubscriberTemplate;
import org.etd.framework.starter.event.server.eventbus.kafka.BatchKafkaEventBusSubscriber;
import org.etd.framework.starter.event.server.eventbus.kafka.KafkaEventBusSubscriberAdapter;
import org.etd.framework.starter.event.server.eventbus.kafka.SingleKafkaEventBusSubscriber;
import org.etd.framework.starter.event.server.eventbus.port.EventMessageRegistrar;
import org.etd.framework.starter.event.server.eventbus.port.EventSubscriptionResolver;
import org.etd.framework.starter.event.server.eventbus.port.EventTypeResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.util.Assert;

/**
 * 事件总线统一入口消费自动配置。
 */
@AutoConfiguration
@AutoConfigureAfter({EventClientAutoConfiguration.class, EventDeliveryAutoConfiguration.class})
@ConditionalOnClass(Consumer.class)
@EnableConfigurationProperties(EventServerProperties.class)
public class EventServerAutoConfiguration {

    private static final String KAFKA_LISTENER_PROPERTY_PREFIX = "spring.kafka.listener";
    private static final String KAFKA_LISTENER_TYPE_SINGLE = "single";
    private static final String KAFKA_LISTENER_TYPE_BATCH = "batch";

    /** 批量事件监听器专用容器工厂 Bean 名称。 */
    public static final String EVENT_BATCH_LISTENER_CONTAINER_FACTORY =
            "eventBatchKafkaListenerContainerFactory";

    /** 由服务端声明统一入口 Topic。 */
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

    /** 创建事件总线订阅模板。 */
    @Bean
    @ConditionalOnMissingBean(EventBusSubscriberTemplate.class)
    public EventBusSubscriberTemplate eventBusSubscriberTemplate(
            EventTypeResolver eventTypeResolver,
            EventMessageRegistrar messageRegistrar,
            EventSubscriptionResolver subscriptionResolver,
            EventDeliveryTaskRegistrar deliveryTaskRegistrar) {
        return new EventBusSubscriberTemplate(
                eventTypeResolver, messageRegistrar, subscriptionResolver, deliveryTaskRegistrar);
    }

    /** 创建事件总线订阅处理器。 */
    @Bean
    @ConditionalOnMissingBean(EventBusSubscriberProcessor.class)
    public EventBusSubscriberProcessor eventBusSubscriberProcessor(
            EventBusSubscriberTemplate subscriberTemplate,
            EventDeliveryTaskPublisher taskPublisher) {
        return new EventBusSubscriberProcessor(subscriberTemplate, taskPublisher);
    }

    /** 创建 Kafka 入口消费适配器。 */
    @Bean
    @ConditionalOnMissingBean(KafkaEventBusSubscriberAdapter.class)
    public KafkaEventBusSubscriberAdapter kafkaEventBusSubscriberAdapter(
            EventMessageCodec eventMessageCodec,
            EventBusSubscriberProcessor subscriberProcessor) {
        return new KafkaEventBusSubscriberAdapter(eventMessageCodec, subscriberProcessor);
    }

    /** 单条模式下创建逐条事件监听入口。 */
    @Bean
    @ConditionalOnProperty(prefix = KAFKA_LISTENER_PROPERTY_PREFIX, name = "type",
            havingValue = KAFKA_LISTENER_TYPE_SINGLE, matchIfMissing = true)
    public SingleKafkaEventBusSubscriber singleKafkaEventBusSubscriber(
            KafkaEventBusSubscriberAdapter subscriberAdapter) {
        return new SingleKafkaEventBusSubscriber(subscriberAdapter);
    }

    /** 创建批量监听专用容器工厂。 */
    @Bean(name = EVENT_BATCH_LISTENER_CONTAINER_FACTORY)
    @ConditionalOnProperty(prefix = KAFKA_LISTENER_PROPERTY_PREFIX, name = "type",
            havingValue = KAFKA_LISTENER_TYPE_BATCH)
    public ConcurrentKafkaListenerContainerFactory<Object, Object>
            eventBatchKafkaListenerContainerFactory(
                    ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
                    ConsumerFactory<Object, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory);
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        return factory;
    }

    /** 批量模式下创建集合事件监听入口。 */
    @Bean
    @ConditionalOnProperty(prefix = KAFKA_LISTENER_PROPERTY_PREFIX, name = "type",
            havingValue = KAFKA_LISTENER_TYPE_BATCH)
    public BatchKafkaEventBusSubscriber batchKafkaEventBusSubscriber(
            KafkaEventBusSubscriberAdapter subscriberAdapter) {
        return new BatchKafkaEventBusSubscriber(subscriberAdapter);
    }

    private void validateTopicProperties(EventBusProperties busProperties,
                                         EventServerProperties serverProperties) {
        Assert.hasText(busProperties.getTopic(), "事件总线入口 Topic 不能为空");
        Assert.isTrue(serverProperties.getPartitions() > 0,
                "事件总线入口 Topic 分区数必须大于 0");
        Assert.isTrue(serverProperties.getReplicas() > 0,
                "事件总线入口 Topic 副本数必须大于 0");
    }
}
