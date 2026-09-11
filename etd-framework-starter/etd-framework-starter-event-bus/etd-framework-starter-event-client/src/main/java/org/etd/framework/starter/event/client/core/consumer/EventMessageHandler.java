package org.etd.framework.starter.event.client.core.consumer;

import org.etd.framework.starter.event.client.core.model.EventMessage;

/**
 * 单条事件消息处理器。
 */
@FunctionalInterface
public interface EventMessageHandler {

    /**
     * 处理一条统一事件消息。
     *
     * @param message 事件消息
     * @throws Exception 业务处理异常
     */
    void handle(EventMessage message) throws Exception;
}
