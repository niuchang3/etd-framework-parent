package org.etd.event.message.controller.vo;

import lombok.Data;
import org.etd.event.delivery.controller.vo.EventDeliveryVO;
import org.etd.event.type.controller.vo.EventTypeVO;

import java.util.List;

/**
 * 事件消息聚合详情，将类型定义和各业务订阅的投递结果串联返回。
 */
@Data
public class EventMessageDetailVO {
    private EventMessageVO message;
    private EventTypeVO eventType;
    private List<EventDeliveryVO> deliveryList;
}
