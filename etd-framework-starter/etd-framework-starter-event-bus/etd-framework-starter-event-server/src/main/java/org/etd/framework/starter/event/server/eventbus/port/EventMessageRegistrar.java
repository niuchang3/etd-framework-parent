package org.etd.framework.starter.event.server.eventbus.port;

import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.eventbus.model.EventMessageRegistration;

/**
 * 入口事件消息幂等登记端口。
 */
@FunctionalInterface
public interface EventMessageRegistrar {

    /**
     * 首次接收时登记消息，重复接收时校验消息内容一致性。
     *
     * @param message 统一事件消息
     * @param eventTypeId 事件类型稳定标识
     * @return 消息登记结果
     */
    EventMessageRegistration registerEventMessage(EventMessage message, Long eventTypeId);
}
