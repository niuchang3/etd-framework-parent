package org.etd.event.message.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.event.message.controller.vo.EventMessageVO;
import org.etd.event.message.entity.EventMessageEntity;
import java.time.Instant;

/**
 * 持久化事件消息的基础读写能力。
 */
public interface EventMessageService {
    IPage<EventMessageVO> page(long current, long size, String eventId, Long eventTypeId,
                               String sourceApplication, Instant startTime, Instant endTime);
    EventMessageVO fetchMessageById(String eventId, Long messageId);

    /**
     * 按事件全局标识查询消息实体，供人工重播恢复原始事件使用。
     *
     * @param eventId 事件全局标识及分片键
     * @return 消息实体，不存在时返回 {@code null}
     */
    EventMessageEntity fetchMessageEntityByEventId(String eventId);

}
