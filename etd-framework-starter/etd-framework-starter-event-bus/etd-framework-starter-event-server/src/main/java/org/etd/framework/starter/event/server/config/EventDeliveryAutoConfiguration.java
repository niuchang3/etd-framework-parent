package org.etd.framework.starter.event.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.etd.framework.event.core.sender.EventMessageSender;
import org.etd.framework.starter.event.client.config.EventClientAutoConfiguration;
import org.etd.framework.starter.event.server.delivery.codec.EventDeliveryCodec;
import org.etd.framework.starter.event.server.delivery.consumer.EventDeliveryStatusListener;
import org.etd.framework.starter.event.server.delivery.consumer.EventDeliveryTaskListener;
import org.etd.framework.starter.event.server.delivery.port.EventDeliveryTaskRegistrar;
import org.etd.framework.starter.event.server.delivery.producer.EventDeliveryStatusPublisher;
import org.etd.framework.starter.event.server.delivery.producer.EventDeliveryTaskPublisher;
import org.etd.framework.starter.event.server.delivery.producer.KafkaEventDeliveryTaskPublisher;
import org.etd.framework.starter.event.server.delivery.retry.EventDeliveryDeadLetterRecoverer;
import org.etd.framework.starter.event.server.delivery.retry.EventDeliveryRetryPolicy;
import org.etd.framework.starter.event.server.delivery.retry.EventDeliveryRetryReporter;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultAfterRollbackProcessor;
import org.springframework.util.Assert;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * 事件订阅可靠投递自动配置。
 */
@AutoConfiguration
@AutoConfigureAfter(EventClientAutoConfiguration.class)
@ConditionalOnClass(Consumer.class)
@EnableConfigurationProperties(EventServerProperties.class)
public class EventDeliveryAutoConfiguration {

    /** 投递任务事务监听器专用容器工厂 Bean 名称。 */
    public static final String EVENT_DELIVERY_TASK_LISTENER_CONTAINER_FACTORY =
            "eventDeliveryTaskKafkaListenerContainerFactory";

    /** 投递状态单条监听器专用容器工厂 Bean 名称。 */
    public static final String EVENT_DELIVERY_STATUS_LISTENER_CONTAINER_FACTORY =
            "eventDeliveryStatusKafkaListenerContainerFactory";

    /** 声明投递任务、状态和死信三个内部 Topic。 */
    @Bean(name = "eventDeliveryInternalTopics")
    @ConditionalOnProperty(prefix = "etd.event.bus.server", name = "create-topic",
            havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "eventDeliveryInternalTopics")
    public KafkaAdmin.NewTopics eventDeliveryInternalTopics(
            EventServerProperties serverProperties) {
        EventServerProperties.Delivery delivery = serverProperties.getDelivery();
        validateDeliveryProperties(delivery);
        return new KafkaAdmin.NewTopics(
                createTopic(delivery.getTaskTopic(), delivery),
                createTopic(delivery.getStatusTopic(), delivery),
                createTopic(delivery.getDeadLetterTopic(), delivery));
    }

    /** 创建内部投递协议编解码器。 */
    @Bean
    @ConditionalOnMissingBean(EventDeliveryCodec.class)
    public EventDeliveryCodec eventDeliveryCodec(ObjectMapper objectMapper) {
        return new EventDeliveryCodec(objectMapper);
    }

    /** 创建内部投递任务 Kafka 发布器。 */
    @Bean
    @ConditionalOnMissingBean(EventDeliveryTaskPublisher.class)
    public EventDeliveryTaskPublisher eventDeliveryTaskPublisher(
            KafkaTemplate<Object, Object> kafkaTemplate,
            EventDeliveryCodec deliveryCodec,
            EventServerProperties serverProperties) {
        Assert.isTrue(kafkaTemplate.isTransactional(),
                "Event Server 必须配置 spring.kafka.producer.transaction-id-prefix");
        return new KafkaEventDeliveryTaskPublisher(
                kafkaTemplate, deliveryCodec, serverProperties.getDelivery().getTaskTopic());
    }

    /** 创建内部投递状态 Kafka 发布器。 */
    @Bean
    @ConditionalOnMissingBean(EventDeliveryStatusPublisher.class)
    public EventDeliveryStatusPublisher eventDeliveryStatusPublisher(
            KafkaTemplate<Object, Object> kafkaTemplate,
            EventDeliveryCodec deliveryCodec,
            EventServerProperties serverProperties) {
        return new EventDeliveryStatusPublisher(
                kafkaTemplate, deliveryCodec, serverProperties.getDelivery().getStatusTopic());
    }

    /** 创建包含事务回滚重试与死信恢复的投递任务监听容器工厂。 */
    @Bean(name = EVENT_DELIVERY_TASK_LISTENER_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<Object, Object>
            eventDeliveryTaskKafkaListenerContainerFactory(
                    ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
                    ConsumerFactory<Object, Object> consumerFactory,
                    KafkaTemplate<Object, Object> kafkaTemplate,
                    EventDeliveryCodec deliveryCodec,
                    EventDeliveryStatusPublisher statusPublisher,
                    EventServerProperties serverProperties) {
        EventServerProperties.Delivery delivery = serverProperties.getDelivery();
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                createRecordFactory(configurer, consumerFactory);
        factory.getContainerProperties().setDeliveryAttemptHeader(true);
        factory.setAfterRollbackProcessor(createAfterRollbackProcessor(
                kafkaTemplate, deliveryCodec, statusPublisher, delivery));
        return factory;
    }

    /** 创建状态投影专用单条监听容器工厂。 */
    @Bean(name = EVENT_DELIVERY_STATUS_LISTENER_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<Object, Object>
            eventDeliveryStatusKafkaListenerContainerFactory(
                    ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
                    ConsumerFactory<Object, Object> consumerFactory) {
        return createRecordFactory(configurer, consumerFactory);
    }

    /** 创建内部投递任务消费者。 */
    @Bean
    @ConditionalOnMissingBean(EventDeliveryTaskListener.class)
    public EventDeliveryTaskListener eventDeliveryTaskListener(
            EventDeliveryCodec deliveryCodec,
            EventMessageSender messageSender,
            EventDeliveryStatusPublisher statusPublisher) {
        return new EventDeliveryTaskListener(deliveryCodec, messageSender, statusPublisher);
    }

    /** 创建投递状态异步投影消费者。 */
    @Bean
    @ConditionalOnMissingBean(EventDeliveryStatusListener.class)
    public EventDeliveryStatusListener eventDeliveryStatusListener(
            EventDeliveryCodec deliveryCodec,
            EventDeliveryTaskRegistrar taskRegistrar) {
        return new EventDeliveryStatusListener(deliveryCodec, taskRegistrar);
    }

    private NewTopic createTopic(
            String topic,
            EventServerProperties.Delivery delivery) {
        return TopicBuilder.name(topic)
                .partitions(delivery.getPartitions())
                .replicas(delivery.getReplicas())
                .build();
    }

    private ConcurrentKafkaListenerContainerFactory<Object, Object> createRecordFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory);
        factory.setBatchListener(false);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    private DefaultAfterRollbackProcessor<Object, Object> createAfterRollbackProcessor(
            KafkaTemplate<Object, Object> kafkaTemplate,
            EventDeliveryCodec deliveryCodec,
            EventDeliveryStatusPublisher statusPublisher,
            EventServerProperties.Delivery delivery) {
        EventDeliveryRetryPolicy retryPolicy = new EventDeliveryRetryPolicy(delivery);
        EventDeliveryDeadLetterRecoverer recoverer = new EventDeliveryDeadLetterRecoverer(
                kafkaTemplate, deliveryCodec, statusPublisher,
                delivery.getDeadLetterTopic(), delivery.getMaxAttempts());
        DefaultAfterRollbackProcessor<Object, Object> processor =
                new DefaultAfterRollbackProcessor<>(recoverer::recover,
                        createBackOff(delivery), kafkaTemplate, true);
        processor.setCommitRecovered(true);
        processor.setRetryListeners(new EventDeliveryRetryReporter(
                deliveryCodec, statusPublisher, retryPolicy, delivery.getMaxAttempts()));
        return processor;
    }

    private ExponentialBackOff createBackOff(EventServerProperties.Delivery delivery) {
        ExponentialBackOff backOff = new ExponentialBackOff(
                delivery.getInitialBackoff().toMillis(), delivery.getBackoffMultiplier());
        backOff.setMaxInterval(delivery.getMaxBackoff().toMillis());
        backOff.setMaxAttempts(delivery.getMaxAttempts() - 1);
        return backOff;
    }

    private void validateDeliveryProperties(EventServerProperties.Delivery delivery) {
        Assert.hasText(delivery.getTaskTopic(), "事件投递任务 Topic 不能为空");
        Assert.hasText(delivery.getStatusTopic(), "事件投递状态 Topic 不能为空");
        Assert.hasText(delivery.getDeadLetterTopic(), "事件投递死信 Topic 不能为空");
        Assert.isTrue(delivery.getPartitions() > 0, "事件投递内部 Topic 分区数必须大于 0");
        Assert.isTrue(delivery.getReplicas() > 0, "事件投递内部 Topic 副本数必须大于 0");
        Assert.isTrue(delivery.getMaxAttempts() > 0, "事件投递最大尝试次数必须大于 0");
        Assert.isTrue(!delivery.getInitialBackoff().isNegative(),
                "事件投递初始退避时间不能为负数");
        Assert.isTrue(delivery.getBackoffMultiplier() >= 1.0D,
                "事件投递退避倍数不能小于 1");
        Assert.isTrue(!delivery.getMaxBackoff().isNegative(),
                "事件投递最大退避时间不能为负数");
    }
}
