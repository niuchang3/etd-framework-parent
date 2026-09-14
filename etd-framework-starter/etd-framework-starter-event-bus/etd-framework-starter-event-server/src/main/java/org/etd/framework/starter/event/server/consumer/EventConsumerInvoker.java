package org.etd.framework.starter.event.server.consumer;

import org.etd.framework.common.core.context.RequestContextInitializer;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.listener.EventTypeDispatcher;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 单条与批量事件消费调用器，统一控制请求上下文边界。
 */
public class EventConsumerInvoker {

    private final EventTypeDispatcher eventTypeDispatcher;

    public EventConsumerInvoker(EventTypeDispatcher eventTypeDispatcher) {
        this.eventTypeDispatcher = eventTypeDispatcher;
    }

    /**
     * 在事件自身的请求上下文中处理单条消息。
     */
    public void invoke(EventMessage message) throws Exception {
        RequestContextInitializer.init(message.context());
        setTraceIdToMdc();
        try {
            eventTypeDispatcher.dispatch(message);
        } finally {
            cleanContext();
        }
    }

    /**
     * 使用批量拉取、逐条处理模式消费消息，每条消息均具有独立上下文作用域。
     */
    public void invokeEach(List<EventMessage> messages) throws Exception {
        for (EventMessage message : messages) {
            invoke(message);
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
