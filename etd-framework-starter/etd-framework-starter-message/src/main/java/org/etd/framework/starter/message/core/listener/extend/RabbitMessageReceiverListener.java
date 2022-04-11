package org.etd.framework.starter.message.core.listener.extend;

import lombok.extern.slf4j.Slf4j;
import org.etd.framework.starter.message.core.listener.AbstractMessageReceiverListenerHandler;
import org.etd.framework.starter.message.core.model.MessageRequest;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author Young
 * @description
 * @date 2020/9/7
 */
@Slf4j
@Component
@RabbitListener(queues = "etd.default.queue")
public class RabbitMessageReceiverListener extends AbstractMessageReceiverListenerHandler {


    @RabbitHandler
    @Override
    public void invokeProcessHandler(MessageRequest messageRequest) throws IOException {
        super.invokeProcessHandler(messageRequest);
    }
}
