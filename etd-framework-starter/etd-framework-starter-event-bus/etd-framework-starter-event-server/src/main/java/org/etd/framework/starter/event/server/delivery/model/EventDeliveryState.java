package org.etd.framework.starter.event.server.delivery.model;

/**
 * Event Server 内部投递状态协议。
 */
public enum EventDeliveryState {

    /** 正在向目标 Topic 发布。 */
    PUBLISHING,

    /** 已成功写入目标 Topic。 */
    SUCCEEDED,

    /** 当前尝试失败，等待容器回退后重试。 */
    RETRY_WAITING,

    /** 已耗尽自动重试次数并进入死信 Topic。 */
    DEAD
}
