package org.etd.framework.starter.amqp.queue;

import org.etd.framework.starter.amqp.interfaces.impl.DefaultAmqpQueue;
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
@Configuration
public class DefaultQueue {

	/**
	 * 初始化默认交换机
	 */
	@Bean
	DirectExchange createDefaultExchange() {
		DefaultAmqpQueue amqpQueue = new DefaultAmqpQueue();
		return (DirectExchange) ExchangeBuilder
				.directExchange(amqpQueue.getExchange())
				.durable(true)
				.build();
	}


	/**
	 * 初始化默认消息队列
	 */
	@Bean
	public Queue createDefaultQueue() {
		DefaultAmqpQueue amqpQueue = new DefaultAmqpQueue();
		return new Queue(amqpQueue.getQueueName());
	}


	/**
	 * 绑定默认的交换机消息队列
	 */
	@Bean
	Binding orderCancelDirectBinding(DirectExchange createDefaultExchange, Queue createDefaultQueue) {
		DefaultAmqpQueue amqpQueue = new DefaultAmqpQueue();
		return BindingBuilder
				.bind(createDefaultQueue)
				.to(createDefaultExchange)
				.with(amqpQueue.getRouteKey());
	}

}
