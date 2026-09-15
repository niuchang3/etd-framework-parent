package org.etd.event.message.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.event.message.controller.vo.EventMessageVO;
import org.etd.event.message.entity.EventMessageEntity;
import org.etd.framework.event.core.model.EventMessage;

import java.time.Instant;

/**
 * 持久化事件消息只读查询能力。
 */
public interface EventMessageService {
    IPage<EventMessageVO> page(long current, long size, String eventId, Long eventTypeId,
                               String sourceApplication, Instant startTime, Instant endTime);
    EventMessageVO fetchByEventIdAndId(String eventId, Long id);

    /**
     * 按事件全局标识查询持久化实体，用于入口消费幂等判断。
     *
     * @param eventId 事件全局标识及分片键
     * @return 已持久化实体，不存在时返回 {@code null}
     */
    EventMessageEntity fetchMessageEntityByEventId(String eventId);

    /**
     * 将统一事件协议保存为原始消息记录。
     *
     * @param message 统一事件消息
     * @param eventTypeId 事件类型主键
     * @return 消息记录主键
     */
    Long createEventMessage(EventMessage message, Long eventTypeId);
}
