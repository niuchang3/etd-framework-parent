package org.etd.framework.starter.message.core.queue.extend;

import org.etd.framework.starter.message.core.queue.RabbitQueue;


/**
 * @author Administrator
 */

public enum DefaultRabbitQueue implements RabbitQueue {
    /**
     * 提供RabbitMQ默认的消息队列
     */
    DEFAULT("etd.default.exchange", "etd.default.queue", "etd.default.route");

    private String exchange;
    private String queueName;
    private String routeKey;


    DefaultRabbitQueue(String exchange, String queueName, String routeKey) {
        this.exchange = exchange;
        this.queueName = queueName;
        this.routeKey = routeKey;
    }

    /**
     * 获取 Exchange 属性值
     *
     * @return 处理结果
     */
    @Override
    public String getExchange() {
        return this.exchange;
    }

    /**
     * 获取 QueueName 属性值
     *
     * @return 处理结果
     */
    @Override
    public String getQueueName() {
        return this.queueName;
    }

    /**
     * 获取 RouteKey 属性值
     *
     * @return 处理结果
     */
    @Override
    public String getRouteKey() {
        return this.routeKey;
    }
}
