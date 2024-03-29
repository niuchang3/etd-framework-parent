package org.etd.framework.starter.message.core.config;

import org.etd.framework.common.core.constants.RequestContextConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.message.core.context.AbstractRabbitRequestContextInitialization;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 配置RabbitTemplate 以及 SimpleRabbitListenerContainerFactory 同时将请求上下文进行透传
 *
 * @author 牛昌
 */
@Configuration
@Import({DefaultQueueConfig.class})
public class RabbitConfig extends AbstractRabbitRequestContextInitialization {


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
            initialization(message);
            return message;
        });
        return factory;
    }

    public void setRabbitMqMessageHeads(Message message) {
        message.getMessageProperties().setHeader(RequestContextConstant.TRACE_ID.getCode(), RequestContext.getTraceId());
        message.getMessageProperties().setHeader(RequestContextConstant.TENANT_CODE.getCode(), RequestContext.getTenantCode());
        message.getMessageProperties().setHeader(RequestContextConstant.TOKEN.getCode(), RequestContext.getTenantCode());
    }
}
