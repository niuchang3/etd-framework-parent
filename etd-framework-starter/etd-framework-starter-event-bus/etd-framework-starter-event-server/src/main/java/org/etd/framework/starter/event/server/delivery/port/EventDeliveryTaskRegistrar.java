package org.etd.framework.starter.event.server.delivery.port;

import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryStatusEvent;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;
import org.etd.framework.starter.event.server.eventbus.model.EventSubscriptionTarget;

import java.util.List;

/**
 * 事件可靠投递任务持久化端口，统一负责任务登记、查询和状态更新。
 */
public interface EventDeliveryTaskRegistrar {

    /**
     * 查询重复入口消息已经登记的投递任务。
     *
     * @param eventId 事件全局标识
     * @param messageId 消息持久化标识
     * @param message 原始事件消息
     * @return 已登记的可靠投递任务列表
     */
    List<EventDeliveryTask> selectDeliveryTaskList(
            String eventId, Long messageId, EventMessage message);

    /**
     * 按业务订阅目标登记可靠投递任务。
     *
     * @param message 原始事件消息
     * @param messageId 消息持久化标识
     * @param targetList 订阅目标快照列表
     * @return 新登记的可靠投递任务列表
     */
    List<EventDeliveryTask> createDeliveryTaskList(
            EventMessage message,
            Long messageId,
            List<EventSubscriptionTarget> targetList);

    /**
     * 幂等更新可靠投递任务状态。
     *
     * @param statusEvent 投递状态事件
     * @throws Exception 状态持久化失败
     */
    void updateDeliveryStatus(EventDeliveryStatusEvent statusEvent) throws Exception;
}
