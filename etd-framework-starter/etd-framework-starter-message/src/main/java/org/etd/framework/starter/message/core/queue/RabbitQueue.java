package org.etd.framework.starter.message.core.queue;

import org.etd.framework.starter.message.core.queue.MessageQueue;

public interface RabbitQueue extends MessageQueue {

    /**
     * 获取交换机
     *
     * @return
     */
    String getExchange();

    /**
     * 获取消息队列名称
     *
     * @return
     */
    String getQueueName();

    /**
     * 获取路由Key
     *
     * @return
     */
    String getRouteKey();
}
