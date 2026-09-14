package org.etd.event.biz;

import org.etd.event.delivery.service.EventDeliveryService;
import org.etd.event.message.controller.vo.EventMessageDetailVO;
import org.etd.event.message.controller.vo.EventMessageVO;
import org.etd.event.message.service.EventMessageService;
import org.etd.event.subscription.controller.dto.EventSubscriptionSaveDTO;
import org.etd.event.subscription.controller.dto.EventSubscriptionUpdateDTO;
import org.etd.event.subscription.service.EventSubscriptionService;
import org.etd.event.type.entity.EventTypeEntity;
import org.etd.event.type.service.EventTypeService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 事件中心管理用例编排，维护事件类型、订阅和运行记录之间的业务约束。
 */
@Service
public class EventManagementBizService {

    private final EventTypeService eventTypeService;
    private final EventSubscriptionService subscriptionService;
    private final EventMessageService messageService;
    private final EventDeliveryService deliveryService;

    public EventManagementBizService(EventTypeService eventTypeService,
                                     EventSubscriptionService subscriptionService,
                                     EventMessageService messageService,
                                     EventDeliveryService deliveryService) {
        this.eventTypeService = eventTypeService;
        this.subscriptionService = subscriptionService;
        this.messageService = messageService;
        this.deliveryService = deliveryService;
    }

    /**
     * 创建订阅前确认目标事件类型存在且处于启用状态。
     *
     * @param dto 订阅配置
     * @return 新订阅主键
     */
    public Long createSubscription(EventSubscriptionSaveDTO dto) {
        requireEnabledEventType(dto.getEventTypeId());
        return subscriptionService.create(dto);
    }

    /**
     * 更新订阅前确认新选择的事件类型可被订阅。
     *
     * @param id 订阅主键
     * @param dto 更新内容及乐观锁版本
     * @return 是否更新成功
     */
    public boolean modifySubscription(Long id, EventSubscriptionUpdateDTO dto) {
        requireEnabledEventType(dto.getContent().getEventTypeId());
        return subscriptionService.modify(id, dto);
    }

    /**
     * 删除没有任何订阅引用的事件类型。
     *
     * @param id 事件类型主键
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean removeEventType(Long id) {
        eventTypeService.requireEntity(id);
        if (subscriptionService.existsByEventTypeId(id)) {
            throw new ApiRuntimeException("事件类型仍存在订阅配置，不能删除。");
        }
        return eventTypeService.remove(id);
    }

    /**
     * 聚合消息、类型定义以及面向各业务应用的投递结果。
     *
     * @param eventId 事件全局标识，同时作为物理分表路由键
     * @param messageId 消息主键
     * @return 消息聚合详情
     */
    public EventMessageDetailVO fetchMessageDetail(String eventId, Long messageId) {
        EventMessageVO message = messageService.fetchByEventIdAndId(eventId, messageId);
        EventMessageDetailVO detail = new EventMessageDetailVO();
        detail.setMessage(message);
        detail.setEventType(eventTypeService.fetchById(message.getEventTypeId()));
        detail.setDeliveryList(deliveryService.selectListByMessage(eventId, messageId));
        return detail;
    }

    private void requireEnabledEventType(Long eventTypeId) {
        EventTypeEntity eventType = eventTypeService.requireEntity(eventTypeId);
        if (eventType.getDataStatus() != BasicConstant.DataStatus.ENABLED_CODE) {
            throw new ApiRuntimeException("只能订阅已启用的事件类型。");
        }
    }
}
