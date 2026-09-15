package org.etd.framework.starter.event.server.persistence;

import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.listener.EventTypeListener;
import org.springframework.core.Ordered;

import java.util.Set;

/**
 * 将所有入口事件优先交给业务持久化端口的内部监听器。
 */
public class EventPersistenceListener implements EventTypeListener, Ordered {

    private final EventMessagePersistence eventMessagePersistence;

    public EventPersistenceListener(EventMessagePersistence eventMessagePersistence) {
        this.eventMessagePersistence = eventMessagePersistence;
    }

    /**
     * 持久化属于事件中心的公共前置步骤，因此匹配全部事件类型。
     *
     * @return 全局事件类型集合
     */
    @Override
    public Set<String> eventTypes() {
        return Set.of(ALL_EVENT_TYPES);
    }

    /**
     * 调用业务持久化实现；异常继续上抛以阻止消费成功确认。
     *
     * @param message 统一事件消息
     * @throws Exception 持久化失败
     */
    @Override
    public void onEvent(EventMessage message) throws Exception {
        eventMessagePersistence.persist(message);
    }

    /**
     * 确保持久化先于应用注册的其他观察或处理监听器执行。
     *
     * @return Spring 最高执行优先级
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
