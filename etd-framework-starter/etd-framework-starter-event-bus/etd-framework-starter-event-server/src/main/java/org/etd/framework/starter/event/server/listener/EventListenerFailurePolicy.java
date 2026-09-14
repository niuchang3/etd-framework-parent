package org.etd.framework.starter.event.server.listener;

/**
 * 事件监听器失败处理策略。
 */
public enum EventListenerFailurePolicy {

    /**
     * 监听器失败时中断当前事件处理，由 Kafka 消费链路负责重试。
     */
    RETRY_EVENT,

    /**
     * 监听器失败时只记录日志，并继续执行当前事件的其他监听器。
     */
    IGNORE_FAILURE
}
