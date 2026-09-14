package org.etd.framework.starter.event.server.listener;

import org.etd.framework.event.core.model.EventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 按 {@code eventType} 将统一事件分发给已注册的服务端监听器。
 */
public class EventTypeDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventTypeDispatcher.class);

    private final List<ListenerRegistration> registrations;

    public EventTypeDispatcher(List<EventTypeListener> listeners) {
        List<EventTypeListener> orderedListeners = new ArrayList<>(listeners);
        AnnotationAwareOrderComparator.sort(orderedListeners);
        this.registrations = createRegistrations(orderedListeners);
    }

    /**
     * 将一条事件依次分发给精确匹配和全局监听器。
     *
     * @param message 统一事件消息
     * @throws Exception 要求重试的监听器执行失败
     */
    public void dispatch(EventMessage message) throws Exception {
        boolean matched = false;
        for (ListenerRegistration registration : registrations) {
            if (!registration.supports(message.eventType())) {
                continue;
            }
            matched = true;
            invokeListener(registration.listener(), message);
        }
        if (!matched) {
            LOGGER.warn("事件未找到监听器 eventId={}, eventType={}, source={}",
                    message.eventId(), message.eventType(), message.source());
        }
    }

    private List<ListenerRegistration> createRegistrations(List<EventTypeListener> listeners) {
        List<ListenerRegistration> result = new ArrayList<>(listeners.size());
        for (EventTypeListener listener : listeners) {
            Assert.notNull(listener, "事件监听器不能为空");
            Set<String> eventTypes = validateEventTypes(listener);
            Assert.notNull(listener.failurePolicy(), "事件监听器失败策略不能为空");
            result.add(new ListenerRegistration(listener, eventTypes));
        }
        return List.copyOf(result);
    }

    private Set<String> validateEventTypes(EventTypeListener listener) {
        Set<String> eventTypes = listener.eventTypes();
        Assert.notEmpty(eventTypes, "事件监听器必须声明至少一个事件类型");
        Set<String> validatedEventTypes = new HashSet<>(eventTypes.size());
        for (String eventType : eventTypes) {
            Assert.hasText(eventType, "事件监听器声明的事件类型不能为空");
            validatedEventTypes.add(eventType);
        }
        return Set.copyOf(validatedEventTypes);
    }

    private void invokeListener(EventTypeListener listener, EventMessage message) throws Exception {
        try {
            listener.onEvent(message);
        } catch (Exception exception) {
            if (listener.failurePolicy() == EventListenerFailurePolicy.IGNORE_FAILURE) {
                LOGGER.error("观察型事件监听器执行失败 eventId={}, eventType={}, listener={}",
                        message.eventId(), message.eventType(), listener.getClass().getName(), exception);
                return;
            }
            LOGGER.error("事件监听器执行失败，等待消费链路重试 eventId={}, eventType={}, listener={}",
                    message.eventId(), message.eventType(), listener.getClass().getName(), exception);
            throw exception;
        }
    }

    /**
     * 保存启动阶段已校验的监听器及其事件类型，避免消费时重复读取动态集合。
     */
    private record ListenerRegistration(EventTypeListener listener, Set<String> eventTypes) {

        private boolean supports(String eventType) {
            return eventTypes.contains(eventType) || eventTypes.contains(EventTypeListener.ALL_EVENT_TYPES);
        }
    }
}
