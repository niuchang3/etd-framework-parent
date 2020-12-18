package org.etd.framework.starter.amqp.handler;

import org.etd.framework.starter.amqp.model.AmqpMessage;

import java.io.IOException;

/**
 * @author Young
 * @description
 * @date 2020/9/7
 */
public interface MessageHandler {

	/**
	 * 异步处理消息
	 *
	 * @param message
	 * @throws IOException
	 */
	void process(AmqpMessage message) throws IOException;
}
