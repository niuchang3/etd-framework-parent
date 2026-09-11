package org.etd.framework.starter.event.client.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.ConfigException;
import org.etd.framework.starter.event.client.context.KafkaRequestContextProducerInterceptor;
import org.etd.framework.starter.event.client.context.KafkaRequestContextRecordInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
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
     * 声明事件总线统一 Topic，实际创建动作由 Spring Kafka 的 KafkaAdmin 完成。
     */
    @Bean
    @ConditionalOnMissingBean(name = "eventBusTopic")
    public KafkaAdmin.NewTopics eventBusTopic(EventBusProperties properties) {
        return new KafkaAdmin.NewTopics(TopicBuilder.name(properties.getTopic())
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicas())
                .build());
    }

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
