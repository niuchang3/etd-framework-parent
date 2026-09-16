package org.etd.event.type.biz;

import org.etd.event.subscription.service.EventSubscriptionService;
import org.etd.event.type.service.EventTypeService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 事件类型跨订阅约束的业务编排。
 */
@Service
public class EventTypeBizService {

    private final EventTypeService eventTypeService;
    private final EventSubscriptionService subscriptionService;

    public EventTypeBizService(EventTypeService eventTypeService,
                               EventSubscriptionService subscriptionService) {
        this.eventTypeService = eventTypeService;
        this.subscriptionService = subscriptionService;
    }

    /**
     * 删除没有任何订阅引用的事件类型。
     *
     * @param eventTypeId 事件类型主键
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean removeEventType(Long eventTypeId) {
        eventTypeService.requireEntity(eventTypeId);
        if (subscriptionService.existsByEventTypeId(eventTypeId)) {
            throw new ApiRuntimeException("事件类型仍存在订阅配置，不能删除。");
        }
        return eventTypeService.remove(eventTypeId);
    }
}
