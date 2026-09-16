package org.etd.event.delivery.converter;

import org.etd.event.delivery.entity.EventDeliveryEntity;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 将持久化投递记录转换为 Starter 内部投递协议。
 */
@Component
public class EventDeliveryTaskConverter {

    /**
     * 按数据库稳定主键构造投递任务列表。
     *
     * @param message 原始事件消息
     * @param deliveryList 已持久化的订阅投递记录
     * @return 可发布到内部 Topic 的投递任务列表
     */
    public List<EventDeliveryTask> convertDeliveryTaskList(
            EventMessage message,
            List<EventDeliveryEntity> deliveryList) {
        List<EventDeliveryTask> taskList = new ArrayList<>(deliveryList.size());
        for (EventDeliveryEntity delivery : deliveryList) {
            taskList.add(convertDeliveryTask(message, delivery));
        }
        return List.copyOf(taskList);
    }

    /**
     * 将一条投递记录转换为稳定投递任务。
     *
     * @param message 原始事件消息
     * @param delivery 投递记录
     * @return 内部投递任务
     */
    public EventDeliveryTask convertDeliveryTask(
            EventMessage message,
            EventDeliveryEntity delivery) {
        return new EventDeliveryTask(
                delivery.getId(), delivery.getEventMessageId(), delivery.getSubscriptionId(),
                delivery.getEventId(), delivery.getTargetTopic(), message);
    }
}
