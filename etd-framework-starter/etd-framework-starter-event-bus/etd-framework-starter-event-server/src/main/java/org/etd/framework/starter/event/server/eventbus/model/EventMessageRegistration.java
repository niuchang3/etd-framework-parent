package org.etd.framework.starter.event.server.eventbus.model;

import java.util.Objects;

/**
 * 入口消息登记结果。
 *
 * @param messageId 消息持久化标识
 * @param created 本次是否首次创建消息记录
 */
public record EventMessageRegistration(Long messageId, boolean created) {

    public EventMessageRegistration {
        Objects.requireNonNull(messageId, "消息持久化标识不能为空");
    }
}
