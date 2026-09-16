package org.etd.framework.starter.event.server.eventbus;

import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;

import java.util.List;
import java.util.Objects;

/**
 * 统一入口事件处理结果，携带需要交给可靠投递链路的任务集合。
 *
 * @param deliveryTaskList 可靠投递任务列表
 */
public record EventBusSubscriberResult(List<EventDeliveryTask> deliveryTaskList) {

    public EventBusSubscriberResult {
        deliveryTaskList = List.copyOf(
                Objects.requireNonNull(deliveryTaskList, "投递任务列表不能为空"));
    }
}
