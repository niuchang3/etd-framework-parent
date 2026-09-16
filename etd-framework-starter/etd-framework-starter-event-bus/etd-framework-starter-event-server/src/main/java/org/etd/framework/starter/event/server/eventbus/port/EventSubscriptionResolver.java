package org.etd.framework.starter.event.server.eventbus.port;

import org.etd.framework.starter.event.server.eventbus.model.EventSubscriptionTarget;

import java.util.List;

/**
 * 事件业务订阅目标解析端口。
 */
@FunctionalInterface
public interface EventSubscriptionResolver {

    /**
     * 查询事件首次接收时处于启用状态的订阅目标。
     *
     * @param eventTypeId 事件类型稳定标识
     * @return 订阅目标快照列表
     */
    List<EventSubscriptionTarget> resolveSubscriptionTargetList(Long eventTypeId);
}
