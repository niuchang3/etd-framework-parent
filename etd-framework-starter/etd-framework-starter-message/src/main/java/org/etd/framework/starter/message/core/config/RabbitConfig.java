package org.etd.framework.starter.message.core.config;

import org.aopalliance.intercept.MethodInterceptor;
import org.etd.framework.common.core.context.RequestContextInitializer;
import org.etd.framework.common.core.context.model.RequestContext;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.Map;

/**
 * RabbitMQ 消息队列自动配置类（处理消息发送与接收时的 RequestContext 上下文全量透传）
 *
 * @author Young
 */
@AutoConfiguration
@Import({DefaultQueueConfig.class})
public class RabbitConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate();
        template.setConnectionFactory(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.addBeforePublishPostProcessors(message -> {
            setRabbitMqMessageHeads(message);
            return message;
        });
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setAfterReceivePostProcessors(message -> {
            if (message != null && message.getMessageProperties() != null) {
                RequestContextInitializer.init(message.getMessageProperties().getHeaders());
                MDC.put("traceId", RequestContext.getTraceId());
            }
            return message;
        });
        factory.setAdviceChain(requestContextCleanupAdvice());
        return factory;
    }

    public void setRabbitMqMessageHeads(Message message) {
        if (message != null && message.getMessageProperties() != null) {
            Map<String, Object> headers = RequestContextInitializer.exportMessageHeaders();
            headers.forEach((k, v) -> message.getMessageProperties().setHeader(k, v));
        }
    }

    /**
     * Rabbit 监听线程由线程池复用，必须在成功和异常场景下统一清理请求上下文。
     */
    static MethodInterceptor requestContextCleanupAdvice() {
        return invocation -> {
            try {
                return invocation.proceed();
            } finally {
                RequestContext.clean();
                MDC.clear();
            }
        };
    }
}
