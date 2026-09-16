package org.etd.event.delivery.service;

/**
 * 失败投递记录的人工重播状态管理能力。
 */
public interface EventDeliveryReplayService {

    /**
     * 将等待重试或死信记录重置为待投递状态。
     *
     * @param eventId 原始事件全局标识及分片键
     * @param deliveryId 投递记录主键
     */
    void resetDeliveryForReplay(String eventId, Long deliveryId);
}
