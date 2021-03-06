package org.etd.framework.starter.amqp.listener;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.common.utils.SpringContextHelper;
import org.etd.framework.starter.amqp.handler.MessageHandler;
import org.etd.framework.starter.amqp.model.AmqpMessage;
import org.etd.framework.starter.log.constant.LogConstant;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Young
 * @description
 * @date 2020/9/7
 */
@Slf4j
@Component
@RabbitListener(queues = "etd.default")
public class MessageReceiver {


	@RabbitHandler
	public void process(AmqpMessage message, Channel channel, @Headers Map<String, Object> headers) throws IOException {
		Long deliverTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);

		String traceId = (String) headers.get(LogConstant.TRACE_ID_HEADER);
		Map<String, String> contextMap = new HashMap<>();
		contextMap.put(LogConstant.LOG_TRACE_ID, traceId);
		MDC.setContextMap(contextMap);

		try {
			MessageHandler messageHandler = (MessageHandler) SpringContextHelper.getBean(message.getMessageHandler());
			if (!ObjectUtils.isEmpty(messageHandler)) {
				messageHandler.process(message);
				channel.basicAck(deliverTag, false);
			}
		} catch (Exception e) {
			channel.basicNack(deliverTag, false, false);
		}
	}
}
