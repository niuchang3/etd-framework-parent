package org.etd.framework.starter.message.core.service;

import org.etd.framework.common.core.model.NotificationMsgRequest;
import org.etd.framework.common.core.queue.MessageQueue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Young
 * @description
 * @date 2020/9/7
 */
@Component
public interface RabbitMessageService extends NotificationMessageService, RabbitTemplate.ConfirmCallback,RabbitTemplate.ReturnsCallback {


    /**
     * 发送延迟队列
     * 这种发送方式通常适用于，固定的过期时间
     *
     * @param messageQueue   队列枚举
     * @param notificationMsgRequest 队列消息
     * @param ttl            过期时间
     */
    void sendDelayedMessage(MessageQueue messageQueue, NotificationMsgRequest notificationMsgRequest, Long ttl);

    /**
     * 以插件（rabbitmq_delayed_message_exchange）形式发送延迟队列：
     * 这种发送方式通常适用于灵活不确定发送时间
     *
     * @param messageQueue   队列枚举
     * @param notificationMsgRequest 队列消息
     * @param ttl            过期时间
     */
    void sendPluginDelayedMessage(MessageQueue messageQueue, NotificationMsgRequest notificationMsgRequest, Long ttl);

}
