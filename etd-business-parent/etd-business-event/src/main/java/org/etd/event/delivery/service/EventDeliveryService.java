package org.etd.event.delivery.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.event.delivery.controller.vo.EventDeliveryVO;
import org.etd.event.delivery.entity.EventDeliveryEntity;

import java.time.Instant;
import java.util.List;

/**
 * 事件投递记录查询能力。
 */
public interface EventDeliveryService {
    IPage<EventDeliveryVO> page(long current, long size, String eventId, Long subscriptionId,
                                Integer deliveryStatus, Instant startTime, Instant endTime);

    /**
     * 查询一条投递记录详情。
     *
     * @param eventId 原始事件全局标识及分片键
     * @param deliveryId 投递记录主键
     * @return 投递记录详情
     */
    EventDeliveryVO fetchDeliveryById(String eventId, Long deliveryId);

    /**
     * 按事件分片键查询投递实体。
     *
     * @param eventId 原始事件全局标识及分片键
     * @param deliveryId 投递记录主键
     * @return 投递实体
     */
    EventDeliveryEntity requireDeliveryById(String eventId, Long deliveryId);

    /**
     * 查询一条原始消息对应的全部投递结果。
     *
     * @param eventId 原始事件全局标识及分片键
     * @param eventMessageId 原始消息记录主键
     * @return 投递结果列表
     */
    List<EventDeliveryVO> selectDeliveryListByMessageId(String eventId, Long eventMessageId);
}
