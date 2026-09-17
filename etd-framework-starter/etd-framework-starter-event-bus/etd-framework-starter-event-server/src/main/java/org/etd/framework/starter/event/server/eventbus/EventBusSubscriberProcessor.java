package org.etd.framework.starter.event.server.eventbus;

import org.etd.framework.common.core.context.RequestContextInitializer;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.delivery.producer.EventDeliveryTaskPublisher;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 统一入口事件处理器，负责建立消息上下文并衔接业务接收与可靠投递链路。
 */
public class EventBusSubscriberProcessor {

    private final EventBusSubscriberTemplate subscriberTemplate;
    private final EventDeliveryTaskPublisher taskPublisher;

    public EventBusSubscriberProcessor(EventBusSubscriberTemplate subscriberTemplate,
                                       EventDeliveryTaskPublisher taskPublisher) {
        this.subscriberTemplate = subscriberTemplate;
        this.taskPublisher = taskPublisher;
    }

    /**
     * 在事件自身的请求上下文中处理一条入口消息。
     *
     * @param message 统一事件消息
     * @throws Exception 业务接收或投递任务发布失败
     */
    public void processEvent(EventMessage message) throws Exception {
        RequestContextInitializer.init(message.context());
        setTraceIdToMdc();
        try {
            EventBusSubscriberResult result = subscriberTemplate.receiveEvent(message);
            if (!result.deliveryTaskList().isEmpty()) {
                taskPublisher.publishDeliveryTaskList(result.deliveryTaskList());
            }
        } finally {
            cleanContext();
        }
    }

    /**
     * 逐条处理一次批量拉取中的事件，保证每条消息使用独立上下文。
     *
     * @param messageList 统一事件消息列表
     * @throws Exception 任意消息处理失败
     */
    public void processEventList(List<EventMessage> messageList) throws Exception {
        for (EventMessage message : messageList) {
            processEvent(message);
        }
    }

    private void setTraceIdToMdc() {
        if (StringUtils.hasText(RequestContext.getTraceId())) {
            MDC.put("traceId", RequestContext.getTraceId());
        }
    }

    private void cleanContext() {
        RequestContext.clean();
        MDC.clear();
    }
}
