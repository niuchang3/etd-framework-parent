package org.etd.event.biz;

import org.etd.event.delivery.controller.vo.EventDeliveryVO;
import org.etd.event.delivery.service.EventDeliveryService;
import org.etd.event.message.controller.vo.EventMessageDetailVO;
import org.etd.event.message.controller.vo.EventMessageVO;
import org.etd.event.message.service.EventMessageService;
import org.etd.event.subscription.service.EventSubscriptionService;
import org.etd.event.type.controller.vo.EventTypeVO;
import org.etd.event.type.service.EventTypeService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 事件管理跨表查询编排测试。
 */
class EventManagementBizServiceTest {

    @Test
    void shouldAggregateMessageTypeAndTargetDeliveries() {
        EventTypeService typeService = mock(EventTypeService.class);
        EventSubscriptionService subscriptionService = mock(EventSubscriptionService.class);
        EventMessageService messageService = mock(EventMessageService.class);
        EventDeliveryService deliveryService = mock(EventDeliveryService.class);
        EventManagementBizService service = new EventManagementBizService(
                typeService, subscriptionService, messageService, deliveryService);
        EventMessageVO message = new EventMessageVO();
        message.setId(10L);
        message.setEventTypeId(20L);
        EventTypeVO type = new EventTypeVO();
        List<EventDeliveryVO> deliveryList = List.of(new EventDeliveryVO());
        when(messageService.fetchByShardingKeyAndId((short) 2, 10L)).thenReturn(message);
        when(typeService.fetchById(20L)).thenReturn(type);
        when(deliveryService.selectListByMessage((short) 2, 10L)).thenReturn(deliveryList);

        EventMessageDetailVO detail = service.fetchMessageDetail((short) 2, 10L);

        assertSame(message, detail.getMessage());
        assertSame(type, detail.getEventType());
        assertEquals(deliveryList, detail.getDeliveryList());
    }
}
