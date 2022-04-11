package org.etd.framework.starter.message.core.queue.extend;

import org.etd.framework.starter.message.core.queue.RabbitQueue;


public enum DefaultRabbitMQQueue implements RabbitQueue {
    /**
     * 提供RabbitMQ默认的消息队列
     */
    DEFAULT("etd.default.exchange", "etd.default.queue", "etd.default.route");

    private String exchange;
    private String queueName;
    private String routeKey;


    DefaultRabbitMQQueue(String exchange, String queueName, String routeKey) {
        this.exchange = exchange;
        this.queueName = queueName;
        this.routeKey = routeKey;
    }

    @Override
    public String getExchange() {
        return this.exchange;
    }

    @Override
    public String getQueueName() {
        return this.queueName;
    }

    @Override
    public String getRouteKey() {
        return this.routeKey;
    }
}
