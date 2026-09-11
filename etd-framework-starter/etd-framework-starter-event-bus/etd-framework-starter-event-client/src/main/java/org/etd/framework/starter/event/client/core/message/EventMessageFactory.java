package org.etd.framework.starter.event.client.core.message;

import org.etd.framework.starter.event.client.core.model.EventMessage;

/**
 * 统一事件消息工厂，在发布边界补齐总线元数据。
 */
public interface EventMessageFactory {

    /**
     * 按首个协议版本创建事件消息。
     *
     * @param eventType    事件类型
     * @param partitionKey 分区键
     * @param payload      业务事件数据
     * @return 统一事件消息
     */
    EventMessage create(String eventType, String partitionKey, Object payload);

    /**
     * 按指定协议版本创建事件消息。
     *
     * @param eventType    事件类型
     * @param eventVersion 事件版本
     * @param partitionKey 分区键
     * @param payload      业务事件数据
     * @return 统一事件消息
     */
    EventMessage create(String eventType, int eventVersion, String partitionKey, Object payload);
}
