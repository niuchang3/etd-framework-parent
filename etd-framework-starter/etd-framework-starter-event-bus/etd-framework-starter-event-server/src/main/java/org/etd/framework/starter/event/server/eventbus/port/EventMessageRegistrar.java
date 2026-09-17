package org.etd.framework.starter.event.server.eventbus.port;

import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.eventbus.model.EventMessageRegistration;
import org.etd.framework.starter.event.server.eventbus.model.EventMessageStatus;

/**
 * 入口事件消息查询与持久化端口。
 */
public interface EventMessageRegistrar {

    /**
     * 按事件标识查询已经持久化的入口消息。
     *
     * @param eventId 事件全局唯一标识及分片键
     * @return 已持久化消息快照，不存在时返回 {@code null}
     */
    EventMessageRegistration selectEventMessage(String eventId);

    /**
     * 按 starter-server 已确定的校验结果创建入口消息。
     *
     * @param message 原始事件消息
     * @param eventTypeId 事件类型主键，类型无法解析时为空
     * @param status 消息处理状态
     * @param failureReason 业务校验失败原因，正常消息为空
     * @return 新建消息的持久化标识
     */
    Long createEventMessage(EventMessage message,
                            Long eventTypeId,
                            EventMessageStatus status,
                            String failureReason);
}
