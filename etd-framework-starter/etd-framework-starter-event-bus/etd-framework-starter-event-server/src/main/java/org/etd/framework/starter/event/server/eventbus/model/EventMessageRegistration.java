package org.etd.framework.starter.event.server.eventbus.model;

import org.etd.framework.event.core.model.EventMessage;

import java.util.Objects;

/**
 * 已持久化入口消息快照，供 starter-server 执行幂等与状态判断。
 *
 * @param messageId 消息持久化标识
 * @param message 原始事件消息
 * @param eventTypeId 已解析的事件类型主键，异常消息可能为空
 * @param status 消息处理状态
 * @param failureReason 业务校验失败原因，正常消息为空
 */
public record EventMessageRegistration(
        Long messageId,
        EventMessage message,
        Long eventTypeId,
        EventMessageStatus status,
        String failureReason
) {

    public EventMessageRegistration {
        Objects.requireNonNull(messageId, "消息持久化标识不能为空");
        Objects.requireNonNull(message, "原始事件消息不能为空");
        Objects.requireNonNull(status, "入口事件消息状态不能为空");
    }
}
