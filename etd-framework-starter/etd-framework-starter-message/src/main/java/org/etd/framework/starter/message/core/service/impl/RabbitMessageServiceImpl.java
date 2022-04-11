package org.etd.framework.starter.message.core.service.impl;

import org.etd.framework.starter.message.core.model.MessageRequest;
import org.etd.framework.starter.message.core.queue.MessageQueue;
import org.etd.framework.starter.message.core.queue.RabbitQueue;
import org.etd.framework.starter.message.core.service.RabbitMessageService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RabbitMessageServiceImpl implements RabbitMessageService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送普通队列
     *
     * @param messageQueue
     * @param messageRequest
     */
    @Override
    public void sendMessage(MessageQueue messageQueue, MessageRequest messageRequest) {
        RabbitQueue rabbitQueue = (RabbitQueue) messageQueue;
        rabbitTemplate.convertAndSend(rabbitQueue.getQueueName(), messageRequest);
    }


    /**
     * 发送延迟队列
     * 这种发送方式通常适用于，固定的过期时间
     *
     * @param messageQueue   队列枚举
     * @param messageRequest 队列消息
     * @param ttl            过期时间
     */
    @Override
    public void sendDelayedMessage(MessageQueue messageQueue, MessageRequest messageRequest, Long ttl) {
        RabbitQueue rabbitQueue = (RabbitQueue) messageQueue;
        final Long finalTtl = ttl;
        rabbitTemplate.convertAndSend(rabbitQueue.getExchange(), rabbitQueue.getRouteKey(), messageRequest, message -> {
            message.getMessageProperties().setExpiration(finalTtl.toString());
            return message;
        });
    }

    /**
     * 以插件（rabbitmq_delayed_message_exchange）形式发送延迟队列：
     * 这种发送方式通常适用于灵活不确定发送时间
     *
     * @param messageQueue   队列枚举
     * @param messageRequest 队列消息
     * @param ttl            过期时间
     */
    @Override
    public void sendPluginDelayedMessage(MessageQueue messageQueue, MessageRequest messageRequest, Long ttl) {
        RabbitQueue rabbitQueue = (RabbitQueue) messageQueue;
        final Long finalTtl = ttl;
        rabbitTemplate.convertAndSend(rabbitQueue.getExchange(), rabbitQueue.getRouteKey(), messageRequest, message -> {
            message.getMessageProperties().setHeader("x-delay", finalTtl);
            return message;
        });
    }


}