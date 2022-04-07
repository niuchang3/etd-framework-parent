package org.etd.framework.starter.amqp;

import org.etd.framework.starter.log.constant.LogConstant;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Correlation;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Young
 * @description
 * @date 2020/9/7
 */
@Order(0)
@Configuration
@ComponentScan({"org.etd.framework.starter.amqp.*"})
public class AmqpConfig {

	/**
	 * 默认将线程ID传递到下游监听者下游监听者方便日志监听
	 * 	上游消息生产生：
	 * 			Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
	 * 			message.getMessageProperties().setHeader(LogConstant.TRACE_ID_HEADER, copyOfContextMap.get(LogConstant.LOG_TRACE_ID));
	 *
	 * 	下游消息消费者：
	 * 			String traceId = (String) headers.get(LogConstant.TRACE_ID_HEADER);
	 * 			Map<String, String> contextMap = new HashMap<>();
	 * 			contextMap.put(LogConstant.LOG_TRACE_ID, traceId);
	 * 			MDC.setContextMap(contextMap);
	 *
	 * @param connectionFactory
	 * @return
	 */
	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate template = new RabbitTemplate();
		template.setConnectionFactory(connectionFactory);
		template.setBeforePublishPostProcessors(new MessagePostProcessor() {
			@Override
			public Message postProcessMessage(Message message) throws AmqpException {
				Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
				message.getMessageProperties().setHeader(LogConstant.TRACE_ID_HEADER, copyOfContextMap.get(LogConstant.LOG_TRACE_ID));
				return message;
			}

			@Override
			public Message postProcessMessage(Message message, Correlation correlation) {
				Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
				message.getMessageProperties().setHeader(LogConstant.TRACE_ID_HEADER, copyOfContextMap.get(LogConstant.LOG_TRACE_ID));
				return message;
			}
		});
		return template;
	}
}
