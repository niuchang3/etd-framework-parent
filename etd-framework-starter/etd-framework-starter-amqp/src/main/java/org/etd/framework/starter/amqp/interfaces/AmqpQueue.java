package org.etd.framework.starter.amqp.interfaces;

/**
 * @author Young
 * @description
 * @date 2020/9/7
 */

public interface AmqpQueue {

	/**
	 * 得到交换机
	 *
	 * @return
	 */
	default String getExchange() {
		return "etd.default";
	}

	/**
	 * 得到队列名称
	 *
	 * @return
	 */
	default String getQueueName() {
		return "etd.default";
	}

	/**
	 * 获取路由器Key
	 *
	 * @return
	 */
	default String getRouteKey() {
		return "etd.default";
	}
}
