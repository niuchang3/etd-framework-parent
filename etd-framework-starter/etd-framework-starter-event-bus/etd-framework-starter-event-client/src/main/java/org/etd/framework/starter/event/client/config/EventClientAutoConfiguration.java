package org.etd.framework.starter.event.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.event.core.codec.EventMessageCodec;
import org.etd.framework.event.core.id.EventIdGenerator;
import org.etd.framework.event.core.sender.EventMessageSender;
import org.etd.framework.starter.event.client.core.connection.KafkaConnectionVerifier;
import org.etd.framework.starter.event.client.core.id.SnowflakeEventIdGenerator;
import org.etd.framework.starter.event.client.core.message.DefaultEventMessageFactory;
import org.etd.framework.starter.event.client.core.message.EventMessageFactory;
import org.etd.framework.starter.event.client.core.publisher.DefaultEventPublisher;
import org.etd.framework.starter.event.client.core.publisher.EventPublisher;
import org.etd.framework.starter.event.client.core.publisher.KafkaEventMessageSender;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * 事件总线发送端 Kafka 自动配置。
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(EventClientProperties.class)
public class EventClientAutoConfiguration {

    /**
     * 创建统一事件消息 JSON 编解码器。
     */
    @Bean
    @ConditionalOnMissingBean(EventMessageCodec.class)
    public EventMessageCodec eventMessageCodec(ObjectMapper objectMapper) {
        return new EventMessageCodec(objectMapper);
    }

    /**
     * 创建可被业务应用替换的事件 ID 生成器。
     */
    @Bean
    @ConditionalOnMissingBean(EventIdGenerator.class)
    public EventIdGenerator eventIdGenerator(EventClientProperties properties) {
        EventClientProperties.IdGenerator idGenerator = properties.getIdGenerator();
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
     * 创建可供发布端和服务端转发复用的 Kafka 底层发送器。
     */
    @Bean
    @ConditionalOnMissingBean(EventMessageSender.class)
    public EventMessageSender eventMessageSender(KafkaTemplate<Object, Object> kafkaTemplate,
                                                 EventMessageCodec eventMessageCodec) {
        return new KafkaEventMessageSender(kafkaTemplate, eventMessageCodec);
    }

    /**
     * 创建面向业务的新事件发布入口。
     */
    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    public EventPublisher eventPublisher(EventMessageFactory eventMessageFactory,
                                         EventMessageSender eventMessageSender,
                                         EventClientProperties properties) {
        return new DefaultEventPublisher(eventMessageFactory, eventMessageSender, properties);
    }

    /**
     * 在需要严格检查基础设施可用性时，于启动阶段验证发送端 Kafka 连接和认证。
     */
    @Bean
    @ConditionalOnProperty(prefix = "etd.event.bus.client",
            name = "verify-connection-on-startup", havingValue = "true")
    @ConditionalOnMissingBean(KafkaConnectionVerifier.class)
    public KafkaConnectionVerifier kafkaConnectionVerifier(KafkaAdmin kafkaAdmin,
                                                           EventClientProperties properties) {
        return new KafkaConnectionVerifier(kafkaAdmin, properties.getConnectionTimeout());
    }
}
