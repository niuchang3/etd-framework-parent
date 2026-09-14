package org.etd.framework.starter.event.server.consumer;

import org.etd.framework.event.core.model.EventMessage;

import java.util.List;

/**
 * 批量事件消息处理器。批次不存在统一请求上下文，处理器应从每条消息读取上下文。
 */
@FunctionalInterface
public interface BatchEventMessageHandler {

    /**
     * 处理一批统一事件消息。
     *
     * @param messages 事件消息列表
     * @throws Exception 业务处理异常
     */
    void handle(List<EventMessage> messages) throws Exception;
}
