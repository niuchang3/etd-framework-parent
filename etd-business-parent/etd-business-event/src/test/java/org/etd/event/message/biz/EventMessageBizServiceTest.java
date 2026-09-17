package org.etd.event.message.biz;

import org.etd.event.delivery.controller.vo.EventDeliveryVO;
import org.etd.event.delivery.service.EventDeliveryService;
import org.etd.event.message.controller.vo.EventMessageDetailVO;
import org.etd.event.message.controller.vo.EventMessageVO;
import org.etd.event.message.service.EventMessageService;
import org.etd.event.type.controller.vo.EventTypeVO;
import org.etd.event.type.service.EventTypeService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 事件管理跨表查询编排测试。
 */
class EventMessageBizServiceTest {

    @Test
    void shouldAggregateMessageTypeAndTargetDeliveries() {
        EventTypeService typeService = mock(EventTypeService.class);
        EventMessageService messageService = mock(EventMessageService.class);
        EventDeliveryService deliveryService = mock(EventDeliveryService.class);
        EventMessageBizService service = new EventMessageBizService(
                typeService, messageService, deliveryService);
        EventMessageVO message = new EventMessageVO();
        message.setId(10L);
        message.setEventTypeId(20L);
        EventTypeVO type = new EventTypeVO();
        List<EventDeliveryVO> deliveryList = List.of(new EventDeliveryVO());
        when(messageService.fetchMessageById("event-10", 10L)).thenReturn(message);
        when(typeService.fetchById(20L)).thenReturn(type);
        when(deliveryService.selectDeliveryListByMessageId("event-10", 10L)).thenReturn(deliveryList);

        EventMessageDetailVO detail = service.fetchMessageDetail("event-10", 10L);

        assertSame(message, detail.getMessage());
        assertSame(type, detail.getEventType());
        assertEquals(deliveryList, detail.getDeliveryList());
    }

    @Test
    void shouldReturnErrorMessageDetailWithoutTypeDefinition() {
        EventTypeService typeService = mock(EventTypeService.class);
        EventMessageService messageService = mock(EventMessageService.class);
        EventDeliveryService deliveryService = mock(EventDeliveryService.class);
        EventMessageBizService service = new EventMessageBizService(
                typeService, messageService, deliveryService);
        EventMessageVO message = new EventMessageVO();
        message.setId(10L);
        when(messageService.fetchMessageById("event-10", 10L)).thenReturn(message);
        when(deliveryService.selectDeliveryListByMessageId("event-10", 10L))
                .thenReturn(List.of());

        EventMessageDetailVO detail = service.fetchMessageDetail("event-10", 10L);

        assertSame(message, detail.getMessage());
        assertEquals(List.of(), detail.getDeliveryList());
        verifyNoInteractions(typeService);
    }
}
