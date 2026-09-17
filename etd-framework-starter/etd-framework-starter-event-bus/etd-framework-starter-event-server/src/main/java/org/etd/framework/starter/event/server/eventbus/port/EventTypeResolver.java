package org.etd.framework.starter.event.server.eventbus.port;

import org.etd.framework.starter.event.server.eventbus.model.EventTypeDefinition;

/**
 * 入口事件类型事实查询端口。
 */
@FunctionalInterface
public interface EventTypeResolver {

    /**
     * 按类型编码查询事件类型事实快照，不在适配器内执行业务校验。
     *
     * @param eventType 事件类型编码
     * @return 事件类型事实快照，不存在时返回 {@code null}
     */
    EventTypeDefinition selectEventType(String eventType);
}
