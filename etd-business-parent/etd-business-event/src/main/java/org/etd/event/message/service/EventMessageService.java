package org.etd.event.message.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.event.message.controller.vo.EventMessageVO;
import org.etd.event.message.entity.EventMessageEntity;
import org.etd.framework.starter.event.server.eventbus.model.EventMessageStatus;

import java.time.Instant;

/**
 * 持久化事件消息的基础读写能力。
 */
public interface EventMessageService {

    /**
     * 按入口消息属性和处理状态分页查询消息。
     *
     * @param current 当前页码
     * @param size 每页数量
     * @param eventId 事件全局唯一标识
     * @param eventType 原始事件类型编码
     * @param eventTypeId 已解析的事件类型主键
     * @param messageStatus 消息处理状态
     * @param sourceApplication 来源应用
     * @param startTime 接收时间范围起点
     * @param endTime 接收时间范围终点
     * @return 事件消息分页结果
     */
    IPage<EventMessageVO> page(long current, long size, String eventId, String eventType,
                               Long eventTypeId, EventMessageStatus messageStatus,
                               String sourceApplication, Instant startTime, Instant endTime);

    /**
     * 按事件标识和消息主键查询消息详情。
     *
     * @param eventId 事件全局唯一标识及分片键
     * @param messageId 消息记录主键
     * @return 事件消息详情
     */
    EventMessageVO fetchMessageById(String eventId, Long messageId);

    /**
     * 按事件全局标识查询消息实体，供人工重播恢复原始事件使用。
     *
     * @param eventId 事件全局标识及分片键
     * @return 消息实体，不存在时返回 {@code null}
     */
    EventMessageEntity fetchMessageEntityByEventId(String eventId);

}
