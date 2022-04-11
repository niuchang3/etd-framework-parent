package org.etd.framework.starter.amqp.service;

import org.etd.framework.starter.amqp.interfaces.AmqpQueue;
import org.etd.framework.starter.amqp.interfaces.impl.DefaultAmqpQueue;
import org.etd.framework.starter.amqp.model.AmqpMessage;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Young
 * @description
 * @date 2020/9/7
 */
@Service
public class AmqpMessageService {

    @Autowired
    private AmqpTemplate rabbitTemplate;

    /**
     * 发送默认消息队列
     *
     * @param message
     */
    public void sendDefaultMessage(AmqpMessage message) {
        DefaultAmqpQueue defaultAmqpQueue = new DefaultAmqpQueue();
        rabbitTemplate.convertAndSend(defaultAmqpQueue.getQueueName(), message);
    }

    /**
     * 发送普通队列
     *
     * @param queueName
     * @param message
     */
    public void sendMessage(AmqpQueue queueName, AmqpMessage message) {
        rabbitTemplate.convertAndSend(queueName.getQueueName(), message);
    }


    /**
     * 发送延迟队列
     * 这种发送方式通常适用于，固定的过期时间
     *
     * @param queueEnum 队列枚举
     * @param message   队列消息
     * @param ttl       过期时间
     */
    public void sendDelayedMessage(AmqpQueue queueEnum, AmqpMessage message, Long ttl) {
        Long finalTtl = ttl;
        rabbitTemplate.convertAndSend(queueEnum.getExchange(), queueEnum.getRouteKey(), message, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setExpiration(finalTtl.toString());
                return message;
            }
        });
    }

    /**
     * 以插件（rabbitmq_delayed_message_exchange）形式发送延迟队列：
     * 这种发送方式通常适用于灵活不确定发送时间
     *
     * @param queueEnum 队列枚举
     * @param message   队列消息
     * @param ttl       过期时间
     */
    public void sendPluginDelayedMessage(AmqpQueue queueEnum, AmqpMessage message, Long ttl) {
        Long finalTtl = ttl;
        rabbitTemplate.convertAndSend(queueEnum.getExchange(), queueEnum.getRouteKey(), message, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setHeader("x-delay", finalTtl);
                return message;
            }
        });
    }

}
