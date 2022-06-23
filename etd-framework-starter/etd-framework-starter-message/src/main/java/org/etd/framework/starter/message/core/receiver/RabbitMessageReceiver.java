package org.etd.framework.starter.message.core.receiver;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.common.core.listener.ListenerHandlerFactory;
import org.etd.framework.common.core.model.NotificationMsgRequest;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author Young
 * @description
 * @date 2020/9/7
 */
@Slf4j
@Component
@RabbitListener(queues = "etd.default.queue")
public class RabbitMessageReceiver implements ListenerHandlerFactory {


    @RabbitHandler
    public void handleRabbitMessage(NotificationMsgRequest request, Channel channel, Message message) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            getListenerHandler(request.getMessageHandleCode()).invokeProcessHandler(request);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            channel.basicAck(deliveryTag, false);
        }
    }
}
