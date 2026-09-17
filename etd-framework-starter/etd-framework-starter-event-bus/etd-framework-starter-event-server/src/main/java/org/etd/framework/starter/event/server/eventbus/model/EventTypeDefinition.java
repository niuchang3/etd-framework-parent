package org.etd.framework.starter.event.server.eventbus.model;

import java.util.Objects;

/**
 * 事件类型事实快照，由持久化适配器返回并交由 starter-server 执行业务校验。
 *
 * @param eventTypeId 事件类型主键
 * @param eventType 事件类型编码
 * @param sourceApplication 允许产生该事件的来源应用
 * @param latestVersion 当前登记的最新协议版本
 * @param enabled 是否启用
 */
public record EventTypeDefinition(
        Long eventTypeId,
        String eventType,
        String sourceApplication,
        int latestVersion,
        boolean enabled
) {

    public EventTypeDefinition {
        Objects.requireNonNull(eventTypeId, "事件类型主键不能为空");
        Objects.requireNonNull(eventType, "事件类型编码不能为空");
        Objects.requireNonNull(sourceApplication, "事件类型来源应用不能为空");
    }
}
