package org.etd.framework.starter.event.server.listener;

import org.etd.framework.event.core.model.EventMessage;

import java.util.Set;

/**
 * 根据事件类型接收统一事件消息的服务端扩展点。
 *
 * <p>实现类需要注册为 Spring Bean。同一个事件类型可以由多个监听器共同处理，
 * 监听器可通过 {@code Ordered} 或 {@code @Order} 控制执行顺序。</p>
 */
public interface EventTypeListener {

    /**
     * 全局事件类型，声明后可以监听所有事件。
     */
    String ALL_EVENT_TYPES = "*";

    /**
     * 返回当前监听器关心的事件类型。
     *
     * @return 非空事件类型集合
     */
    Set<String> eventTypes();

    /**
     * 在当前事件独立的请求上下文中处理消息。
     *
     * @param message 统一事件消息
     * @throws Exception 监听处理异常
     */
    void onEvent(EventMessage message) throws Exception;

    /**
     * 返回监听器失败时的处理策略。业务处理默认需要触发事件重试。
     *
     * @return 失败处理策略
     */
    default EventListenerFailurePolicy failurePolicy() {
        return EventListenerFailurePolicy.RETRY_EVENT;
    }
}
