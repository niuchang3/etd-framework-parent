package org.etd.framework.starter.event.server.eventbus.port;

import org.etd.framework.event.core.model.EventMessage;

/**
 * 入口事件类型解析端口。
 */
@FunctionalInterface
public interface EventTypeResolver {

    /**
     * 校验事件类型定义并返回其稳定标识。
     *
     * @param message 统一事件消息
     * @return 事件类型稳定标识
     */
    Long resolveEventTypeId(EventMessage message);
}
