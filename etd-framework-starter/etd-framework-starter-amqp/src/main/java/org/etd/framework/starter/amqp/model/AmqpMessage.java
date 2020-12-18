package org.etd.framework.starter.amqp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author Young
 * @description
 * @date 2020/9/7
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AmqpMessage<T extends Serializable> implements Serializable {


	/**
	 *
	 */
	private T body;

	/**
	 * 消息处理器
	 */
	private String messageHandler;

	private String messageId;


}
