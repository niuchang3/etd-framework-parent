package org.etd.event.message.biz;

import org.etd.event.delivery.service.EventDeliveryService;
import org.etd.event.message.controller.vo.EventMessageDetailVO;
import org.etd.event.message.controller.vo.EventMessageVO;
import org.etd.event.message.service.EventMessageService;
import org.etd.event.type.service.EventTypeService;
import org.springframework.stereotype.Service;

/**
 * 事件消息详情查询编排。
 */
@Service
public class EventMessageBizService {

    private final EventTypeService eventTypeService;
    private final EventMessageService messageService;
    private final EventDeliveryService deliveryService;

    public EventMessageBizService(EventTypeService eventTypeService,
                                  EventMessageService messageService,
                                  EventDeliveryService deliveryService) {
        this.eventTypeService = eventTypeService;
        this.messageService = messageService;
        this.deliveryService = deliveryService;
    }

    /**
     * 聚合消息、类型定义以及面向各业务应用的投递结果。
     *
     * @param eventId 事件全局标识，同时作为物理分表路由键
     * @param messageId 消息主键
     * @return 消息聚合详情
     */
    public EventMessageDetailVO fetchMessageDetail(String eventId, Long messageId) {
        EventMessageVO message = messageService.fetchMessageById(eventId, messageId);
        EventMessageDetailVO detail = new EventMessageDetailVO();
        detail.setMessage(message);
        if (message.getEventTypeId() != null) {
            detail.setEventType(eventTypeService.fetchById(message.getEventTypeId()));
        }
        detail.setDeliveryList(deliveryService.selectDeliveryListByMessageId(eventId, messageId));
        return detail;
    }

}
