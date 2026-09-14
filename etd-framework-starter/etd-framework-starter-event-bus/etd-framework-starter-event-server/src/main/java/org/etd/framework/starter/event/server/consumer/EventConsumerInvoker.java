package org.etd.framework.starter.event.server.consumer;

import org.etd.framework.common.core.context.RequestContextInitializer;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.event.core.model.EventMessage;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 单条与批量事件消费调用器，统一控制请求上下文边界。
 */
public class EventConsumerInvoker {

    /**
     * 在事件自身的请求上下文中处理单条消息。
     */
    public void invoke(EventMessage message, EventMessageHandler handler) throws Exception {
        RequestContextInitializer.init(message.context());
        setTraceIdToMdc();
        try {
            handler.handle(message);
        } finally {
            cleanContext();
        }
    }

    /**
     * 处理完整批次。批次可能包含多个上下文，因此不会写入任意一条消息的上下文。
     */
    public void invokeBatch(List<EventMessage> messages, BatchEventMessageHandler handler) throws Exception {
        cleanContext();
        try {
            handler.handle(List.copyOf(messages));
        } finally {
            cleanContext();
        }
    }

    /**
     * 使用批量拉取、逐条处理模式消费消息，每条消息均具有独立上下文作用域。
     */
    public void invokeEach(List<EventMessage> messages, EventMessageHandler handler) throws Exception {
        for (EventMessage message : messages) {
            invoke(message, handler);
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
