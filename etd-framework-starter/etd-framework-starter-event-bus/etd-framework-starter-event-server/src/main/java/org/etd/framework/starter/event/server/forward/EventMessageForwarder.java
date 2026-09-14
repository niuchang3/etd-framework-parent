package org.etd.framework.starter.event.server.forward;

import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.event.core.sender.EventMessageSender;
import org.etd.framework.event.core.sender.EventSendResult;
import org.springframework.util.Assert;

import java.util.concurrent.CompletableFuture;

/**
 * 服务端事件转发器，透明转发完整统一消息，不重新创建事件和请求上下文。
 */
public class EventMessageForwarder {

    private final EventMessageSender eventMessageSender;

    public EventMessageForwarder(EventMessageSender eventMessageSender) {
        this.eventMessageSender = eventMessageSender;
    }

    /**
     * 将已有事件原样发送到目标通道。
     *
     * @param destination 转发目的地
     * @param message     待转发的完整事件消息
     * @return 异步发送结果
     */
    public CompletableFuture<EventSendResult> forward(String destination, EventMessage message) {
        Assert.hasText(destination, "事件转发目的地不能为空");
        Assert.notNull(message, "待转发事件不能为空");
        return eventMessageSender.send(destination, message);
    }
}
