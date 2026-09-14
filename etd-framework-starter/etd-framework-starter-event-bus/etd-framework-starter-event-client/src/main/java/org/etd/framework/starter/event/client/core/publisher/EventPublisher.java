package org.etd.framework.starter.event.client.core.publisher;

/**
 * 事件总线异步发布入口，业务方只提交事件语义和数据。
 */
public interface EventPublisher {

    /**
     * 发布首个协议版本的事件。
     *
     * @param eventType 事件类型
     * @param payload   事件数据
     */
    void publish(String eventType, Object payload);

    /**
     * 按分区键发布首个协议版本的事件。
     *
     * @param eventType    事件类型
     * @param partitionKey 分区键
     * @param payload      事件数据
     */
    void publish(String eventType, String partitionKey, Object payload);

    /**
     * 发布指定协议版本的事件。
     *
     * @param eventType    事件类型
     * @param eventVersion 事件版本
     * @param partitionKey 分区键
     * @param payload      事件数据
     */
    void publish(String eventType, int eventVersion, String partitionKey, Object payload);
}
