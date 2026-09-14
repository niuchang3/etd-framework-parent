package org.etd.framework.event.core.sender;

import org.etd.framework.event.core.model.EventMessage;

import java.util.concurrent.CompletableFuture;

/**
 * 统一事件底层发送能力，既可发送新事件，也可原样转发已有事件。
 */
@FunctionalInterface
public interface EventMessageSender {

    /**
     * 将完整事件消息发送到指定目的地。
     *
     * @param destination 逻辑目的地，Kafka 实现中对应 Topic
     * @param message     完整事件消息
     * @return 异步发送结果
     */
    CompletableFuture<EventSendResult> send(String destination, EventMessage message);
}
