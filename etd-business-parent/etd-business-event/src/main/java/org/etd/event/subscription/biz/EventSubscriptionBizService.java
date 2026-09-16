package org.etd.event.subscription.biz;

import org.etd.event.subscription.controller.dto.EventSubscriptionSaveDTO;
import org.etd.event.subscription.controller.dto.EventSubscriptionUpdateDTO;
import org.etd.event.subscription.service.EventSubscriptionService;
import org.etd.event.type.entity.EventTypeEntity;
import org.etd.event.type.service.EventTypeService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;

/**
 * 事件订阅创建与修改用例编排。
 */
@Service
public class EventSubscriptionBizService {

    private final EventTypeService eventTypeService;
    private final EventSubscriptionService subscriptionService;

    public EventSubscriptionBizService(EventTypeService eventTypeService,
                                       EventSubscriptionService subscriptionService) {
        this.eventTypeService = eventTypeService;
        this.subscriptionService = subscriptionService;
    }

    /**
     * 校验事件类型后创建订阅。
     *
     * @param dto 订阅配置
     * @return 新订阅主键
     */
    public Long createSubscription(EventSubscriptionSaveDTO dto) {
        requireEnabledEventType(dto.getEventTypeId());
        return subscriptionService.create(dto);
    }

    /**
     * 校验事件类型后修改订阅。
     *
     * @param subscriptionId 订阅主键
     * @param dto 更新内容及乐观锁版本
     * @return 是否更新成功
     */
    public boolean modifySubscription(Long subscriptionId, EventSubscriptionUpdateDTO dto) {
        requireEnabledEventType(dto.getContent().getEventTypeId());
        return subscriptionService.modify(subscriptionId, dto);
    }

    private void requireEnabledEventType(Long eventTypeId) {
        EventTypeEntity eventType = eventTypeService.requireEntity(eventTypeId);
        if (eventType.getDataStatus() != BasicConstant.DataStatus.ENABLED_CODE) {
            throw new ApiRuntimeException("只能订阅已启用的事件类型。");
        }
    }
}
