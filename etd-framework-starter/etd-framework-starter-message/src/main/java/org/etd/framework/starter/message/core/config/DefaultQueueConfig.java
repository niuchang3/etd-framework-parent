package org.etd.framework.starter.message.core.config;

import org.etd.framework.starter.message.core.queue.extend.DefaultRabbitMQQueue;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 系统默认Mq信息
 *
 * @author Young
 * @description
 * @date 2020/9/7
 */
public class DefaultQueueConfig {

    /**
     * 初始化默认交换机
     */
    @Bean
    DirectExchange createDefaultExchange() {
        return (DirectExchange) ExchangeBuilder
                .directExchange(DefaultRabbitMQQueue.DEFAULT.getExchange())
                .durable(true)
                .build();
    }


    /**
     * 初始化默认消息队列
     */
    @Bean
    public Queue createDefaultQueue() {
        return new Queue(DefaultRabbitMQQueue.DEFAULT.getQueueName());
    }


    /**
     * 绑定默认的交换机消息队列
     */
    @Bean
    Binding orderCancelDirectBinding(DirectExchange createDefaultExchange, Queue createDefaultQueue) {
        return BindingBuilder
                .bind(createDefaultQueue)
                .to(createDefaultExchange)
                .with(DefaultRabbitMQQueue.DEFAULT.getRouteKey());
    }

}
