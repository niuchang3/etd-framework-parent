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

    /** 原始事件消息。 */
    private EventMessageVO message;

    /** 消息对应的事件类型定义。 */
    private EventTypeVO eventType;

    /** 当前消息面向各业务订阅生成的投递结果。 */
    private List<EventDeliveryVO> deliveryList;
}
