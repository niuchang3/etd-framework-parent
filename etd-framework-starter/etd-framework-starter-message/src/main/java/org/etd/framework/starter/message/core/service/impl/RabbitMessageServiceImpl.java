package org.etd.framework.starter.message.core.service.impl;

import org.etd.framework.common.core.model.NotificationMsgRequest;
import org.etd.framework.common.core.queue.MessageQueue;
import org.etd.framework.starter.message.core.queue.RabbitQueue;
import org.etd.framework.starter.message.core.service.RabbitMessageService;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
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
     * @param notificationMsgRequest
     */
    /**
     * send Message
     *
     * @param messageQueue 参数 messageQueue
     * @param notificationMsgRequest 参数 notificationMsgRequest
     */
    @Override
    public void sendMessage(MessageQueue messageQueue, NotificationMsgRequest notificationMsgRequest) {
        RabbitQueue rabbitQueue = (RabbitQueue) messageQueue;
        rabbitTemplate.convertAndSend(rabbitQueue.getRouteKey(), notificationMsgRequest);
    }


    /**
     * 发送延迟队列
     * 这种发送方式通常适用于，固定的过期时间
     *
     * @param messageQueue   队列枚举
     * @param notificationMsgRequest 队列消息
     * @param ttl            过期时间
     */
    /**
     * send Delayed Message
     *
     * @param messageQueue 参数 messageQueue
     * @param notificationMsgRequest 参数 notificationMsgRequest
     * @param ttl 参数 ttl
     */
    @Override
    public void sendDelayedMessage(MessageQueue messageQueue, NotificationMsgRequest notificationMsgRequest, Long ttl) {
        RabbitQueue rabbitQueue = (RabbitQueue) messageQueue;
        final Long finalTtl = ttl;
        rabbitTemplate.convertAndSend(rabbitQueue.getExchange(), rabbitQueue.getRouteKey(), notificationMsgRequest, message -> {
            message.getMessageProperties().setExpiration(finalTtl.toString());
            return message;
        });
    }

    /**
     * 以插件（rabbitmq_delayed_message_exchange）形式发送延迟队列：
     * 这种发送方式通常适用于灵活不确定发送时间
     *
     * @param messageQueue   队列枚举
     * @param notificationMsgRequest 队列消息
     * @param ttl            过期时间
     */
    /**
     * send Plugin Delayed Message
     *
     * @param messageQueue 参数 messageQueue
     * @param notificationMsgRequest 参数 notificationMsgRequest
     * @param ttl 参数 ttl
     */
    @Override
    public void sendPluginDelayedMessage(MessageQueue messageQueue, NotificationMsgRequest notificationMsgRequest, Long ttl) {
        RabbitQueue rabbitQueue = (RabbitQueue) messageQueue;
        final Long finalTtl = ttl;
        rabbitTemplate.convertAndSend(rabbitQueue.getExchange(), rabbitQueue.getRouteKey(), notificationMsgRequest, message -> {
            message.getMessageProperties().setHeader(org.etd.framework.common.core.constants.HeaderConstant.X_DELAY, finalTtl);
            return message;
        });
    }


    /**
     * confirm
     *
     * @param correlationData 参数 correlationData
     * @param ack 参数 ack
     * @param cause 参数 cause
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {

    }

    /**
     * returned Message
     *
     * @param returned 参数 returned
     */
    @Override
    public void returnedMessage(ReturnedMessage returned) {

    }
}
