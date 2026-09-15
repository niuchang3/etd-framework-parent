package org.etd.event.delivery.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.event.delivery.controller.vo.EventDeliveryVO;
import org.etd.event.subscription.entity.EventSubscriptionEntity;
import org.etd.framework.event.core.model.EventMessage;

import java.time.Instant;
import java.util.List;

/**
 * 事件投递任务查询与人工重播能力。
 */
public interface EventDeliveryService {
    IPage<EventDeliveryVO> page(long current, long size, String eventId, Long subscriptionId,
                                Integer deliveryStatus, Instant startTime, Instant endTime);
    EventDeliveryVO fetchByEventIdAndId(String eventId, Long id);
    List<EventDeliveryVO> selectListByMessage(String eventId, Long eventMessageId);

    /**
     * 按入口事件和启用订阅创建相互独立的待投递任务。
     *
     * @param message 统一事件消息
     * @param eventMessageId 原始消息记录主键
     * @param subscriptionList 创建任务时的启用订阅快照
     */
    void createDeliveryList(EventMessage message, Long eventMessageId,
                            List<EventSubscriptionEntity> subscriptionList);

    boolean replayFailedDelivery(String eventId, Long id);
}
