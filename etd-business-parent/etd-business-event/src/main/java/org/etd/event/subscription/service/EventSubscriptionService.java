package org.etd.event.subscription.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.event.subscription.controller.dto.EventSubscriptionSaveDTO;
import org.etd.event.subscription.controller.dto.EventSubscriptionUpdateDTO;
import org.etd.event.subscription.controller.vo.EventSubscriptionVO;
import org.etd.event.subscription.entity.EventSubscriptionEntity;

import java.util.List;

/**
 * 事件订阅配置基础能力。
 */
public interface EventSubscriptionService {
    IPage<EventSubscriptionVO> page(long current, long size, String keyword, Long eventTypeId,
                                    String subscriberApplication, Boolean enabled);
    List<EventSubscriptionVO> selectEnabledList();

    EventSubscriptionVO fetchById(Long id);
    EventSubscriptionEntity requireEntity(Long id);
    boolean existsByEventTypeId(Long eventTypeId);
    Long create(EventSubscriptionSaveDTO dto);
    boolean modify(Long id, EventSubscriptionUpdateDTO dto);
    boolean switchEnabled(Long id, Boolean enabled);
    boolean remove(Long id);
}
