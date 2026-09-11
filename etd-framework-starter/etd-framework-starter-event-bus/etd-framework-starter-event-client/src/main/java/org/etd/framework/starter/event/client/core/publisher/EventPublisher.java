package org.etd.framework.starter.event.client.core.publisher;

import java.util.concurrent.CompletableFuture;

/**
 * 事件总线发布入口，业务方只提供事件语义和数据。
 */
public interface EventPublisher {

    /**
     * 发布首个协议版本的事件。
     *
     * @param eventType 事件类型
     * @param payload   事件数据
     * @return 发送成功后的事件 ID
     */
    CompletableFuture<String> publish(String eventType, Object payload);

    /**
     * 按分区键发布首个协议版本的事件。
     *
     * @param eventType    事件类型
     * @param partitionKey 分区键
     * @param payload      事件数据
     * @return 发送成功后的事件 ID
     */
    CompletableFuture<String> publish(String eventType, String partitionKey, Object payload);

    /**
     * 发布指定协议版本的事件。
     *
     * @param eventType    事件类型
     * @param eventVersion 事件版本
     * @param partitionKey 分区键
     * @param payload      事件数据
     * @return 发送成功后的事件 ID
     */
    CompletableFuture<String> publish(String eventType, int eventVersion, String partitionKey, Object payload);
}
