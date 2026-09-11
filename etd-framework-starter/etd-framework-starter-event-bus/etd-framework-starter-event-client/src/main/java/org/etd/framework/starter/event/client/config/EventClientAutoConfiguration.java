package org.etd.framework.starter.event.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.ConfigException;
import org.etd.framework.starter.event.client.core.consumer.EventConsumerInvoker;
import org.etd.framework.starter.event.client.core.consumer.EventMessageCodec;
import org.etd.framework.starter.event.client.core.consumer.KafkaEventConsumerAdapter;
import org.etd.framework.starter.event.client.core.connection.KafkaConnectionVerifier;
import org.etd.framework.starter.event.client.core.id.EventIdGenerator;
import org.etd.framework.starter.event.client.core.id.SnowflakeEventIdGenerator;
import org.etd.framework.starter.event.client.core.message.DefaultEventMessageFactory;
import org.etd.framework.starter.event.client.core.message.EventMessageFactory;
import org.etd.framework.starter.event.client.core.publisher.EventPublisher;
import org.etd.framework.starter.event.client.core.publisher.KafkaEventPublisher;
import org.etd.framework.starter.event.client.context.KafkaRequestContextProducerInterceptor;
import org.etd.framework.starter.event.client.context.KafkaRequestContextRecordInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.RecordInterceptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 事件总线客户端 Kafka 自动配置。
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(EventBusProperties.class)
public class EventClientAutoConfiguration {

    private static final String REQUEST_CONTEXT_INTERCEPTOR =
            KafkaRequestContextProducerInterceptor.class.getName();

    /**
     * 将请求上下文生产者拦截器追加到用户已有的 Kafka 拦截器配置中。
     */
    @Bean
    public DefaultKafkaProducerFactoryCustomizer eventRequestContextProducerCustomizer() {
        return producerFactory -> producerFactory.updateConfigs(java.util.Map.of(
                ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
                appendRequestContextInterceptor(producerFactory)));
    }

    /**
     * 消费消息时按记录恢复并清理请求上下文，避免 Kafka 消费线程复用导致上下文泄漏。
     */
    @Bean
    @ConditionalOnMissingBean(RecordInterceptor.class)
    public RecordInterceptor<Object, Object> eventRequestContextRecordInterceptor() {
        return new KafkaRequestContextRecordInterceptor();
    }

    /**
     * 创建可被业务应用替换的事件 ID 生成器。
     */
    @Bean
    @ConditionalOnMissingBean(EventIdGenerator.class)
    public EventIdGenerator eventIdGenerator(EventBusProperties properties) {
        EventBusProperties.IdGenerator idGenerator = properties.getIdGenerator();
        return new SnowflakeEventIdGenerator(idGenerator.getWorkerId(), idGenerator.getDatacenterId());
    }

    /**
     * 创建统一事件消息工厂，来源应用默认读取 spring.application.name。
     */
    @Bean
    @ConditionalOnMissingBean(EventMessageFactory.class)
    public EventMessageFactory eventMessageFactory(EventIdGenerator eventIdGenerator,
                                                   ObjectMapper objectMapper,
                                                   Environment environment) {
        return new DefaultEventMessageFactory(eventIdGenerator, objectMapper,
                environment.getProperty("spring.application.name"));
    }

    /**
     * 创建 Kafka 事件发布器。
     */
    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    public EventPublisher eventPublisher(KafkaTemplate<Object, Object> kafkaTemplate,
                                         ObjectMapper objectMapper,
                                         EventMessageFactory eventMessageFactory,
                                         EventBusProperties properties) {
        return new KafkaEventPublisher(kafkaTemplate, objectMapper, eventMessageFactory, properties);
    }

    /**
     * 创建统一事件消息 JSON 编解码器。
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
     * 在需要严格检查基础设施可用性时，于启动阶段验证 Kafka 连接和 SASL 认证。
     */
    @Bean
    @ConditionalOnProperty(prefix = "etd.event.bus", name = "verify-connection-on-startup", havingValue = "true")
    @ConditionalOnMissingBean(KafkaConnectionVerifier.class)
    public KafkaConnectionVerifier kafkaConnectionVerifier(KafkaAdmin kafkaAdmin,
                                                           EventBusProperties properties) {
        return new KafkaConnectionVerifier(kafkaAdmin, properties.getConnectionTimeout());
    }

    private List<String> appendRequestContextInterceptor(DefaultKafkaProducerFactory<?, ?> producerFactory) {
        Object configured = producerFactory.getConfigurationProperties()
                .get(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG);
        List<String> interceptorClasses = convertInterceptorClasses(configured);
        if (!interceptorClasses.contains(REQUEST_CONTEXT_INTERCEPTOR)) {
            interceptorClasses.add(REQUEST_CONTEXT_INTERCEPTOR);
        }
        return interceptorClasses;
    }

    private List<String> convertInterceptorClasses(Object configured) {
        List<String> interceptorClasses = new ArrayList<>();
        if (configured instanceof Collection<?> collection) {
            for (Object value : collection) {
                interceptorClasses.add(toClassName(value));
            }
        } else if (configured instanceof String value && !value.isBlank()) {
            for (String className : value.split(",")) {
                if (!className.isBlank()) {
                    interceptorClasses.add(className.trim());
                }
            }
        } else if (configured != null) {
            interceptorClasses.add(toClassName(configured));
        }
        return interceptorClasses;
    }

    private String toClassName(Object value) {
        if (value instanceof Class<?> type) {
            return type.getName();
        }
        if (value instanceof String className && !className.isBlank()) {
            return className.trim();
        }
        throw new ConfigException("Kafka producer interceptor 配置必须是类名或 Class 类型");
    }
}
