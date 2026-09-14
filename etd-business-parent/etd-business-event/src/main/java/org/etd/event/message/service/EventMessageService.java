package org.etd.event.message.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.event.message.controller.vo.EventMessageVO;

import java.time.Instant;

/**
 * 持久化事件消息只读查询能力。
 */
public interface EventMessageService {
    IPage<EventMessageVO> page(long current, long size, String eventId, Long eventTypeId,
                               String sourceApplication, Instant startTime, Instant endTime);
    EventMessageVO fetchByEventIdAndId(String eventId, Long id);
}
