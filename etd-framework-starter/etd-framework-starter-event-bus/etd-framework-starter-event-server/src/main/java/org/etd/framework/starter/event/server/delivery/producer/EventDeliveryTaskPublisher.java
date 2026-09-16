package org.etd.framework.starter.event.server.delivery.producer;

import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;

import java.util.List;

/**
 * 内部事件投递任务发布端口。
 */
public interface EventDeliveryTaskPublisher {

    /**
     * 将同一入口事件派生的投递任务写入内部 Topic。
     *
     * @param taskList 投递任务列表；空列表表示当前事件没有业务订阅
     */
    void publishDeliveryTaskList(List<EventDeliveryTask> taskList);

    /**
     * 发布一条人工重播投递任务。
     *
     * @param task 投递任务
     */
    void publishDeliveryTask(EventDeliveryTask task);
}
