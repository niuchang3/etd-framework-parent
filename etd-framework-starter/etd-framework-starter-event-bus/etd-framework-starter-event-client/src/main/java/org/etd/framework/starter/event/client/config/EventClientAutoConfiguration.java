package org.etd.framework.starter.event.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.event.core.codec.EventMessageCodec;
import org.etd.framework.event.core.id.EventIdGenerator;
import org.etd.framework.event.core.sender.EventMessageSender;
import org.etd.framework.starter.event.client.core.async.EventContextTaskDecorator;
import org.etd.framework.starter.event.client.core.aspect.EventAspect;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

/**
 * 事件总线发送端 Kafka 自动配置。
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties({EventBusProperties.class, EventClientProperties.class})
public class EventClientAutoConfiguration {

    public static final String EVENT_BUS_TASK_EXECUTOR = "eventBusTaskExecutor";

    /**
     * 创建事件专用异步线程池，队列满时拒绝任务并由发布入口记录失败日志。
     */
    @Bean(name = EVENT_BUS_TASK_EXECUTOR)
    @ConditionalOnMissingBean(name = EVENT_BUS_TASK_EXECUTOR)
    public ThreadPoolTaskExecutor eventBusTaskExecutor(EventClientProperties properties) {
        EventClientProperties.Async async = properties.getAsync();
        validateAsyncProperties(async);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(async.getCorePoolSize());
        executor.setMaxPoolSize(async.getMaxPoolSize());
        executor.setQueueCapacity(async.getQueueCapacity());
        executor.setThreadNamePrefix("event-bus-sender-");
        executor.setTaskDecorator(new EventContextTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
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
                                         EventBusProperties properties,
                                         @Qualifier(EVENT_BUS_TASK_EXECUTOR) TaskExecutor taskExecutor) {
        Assert.hasText(properties.getTopic(), "事件总线入口 Topic 不能为空");
        return new DefaultEventPublisher(eventMessageFactory, eventMessageSender, properties, taskExecutor);
    }

    /**
     * 创建业务方法注解事件切面。
     */
    @Bean
    @ConditionalOnMissingBean(EventAspect.class)
    public EventAspect eventAspect(EventPublisher eventPublisher) {
        return new EventAspect(eventPublisher);
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

    private void validateAsyncProperties(EventClientProperties.Async async) {
        Assert.isTrue(async.getCorePoolSize() > 0, "事件发送核心线程数必须大于 0");
        Assert.isTrue(async.getMaxPoolSize() >= async.getCorePoolSize(),
                "事件发送最大线程数不能小于核心线程数");
        Assert.isTrue(async.getQueueCapacity() >= 0, "事件发送队列容量不能小于 0");
    }
}
