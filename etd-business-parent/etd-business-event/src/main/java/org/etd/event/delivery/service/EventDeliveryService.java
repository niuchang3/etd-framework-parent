package org.etd.event.delivery.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.event.delivery.controller.vo.EventDeliveryVO;

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
    boolean replayFailedDelivery(String eventId, Long id);
}
