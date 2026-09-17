package org.etd.framework.starter.event.server.eventbus.model;

/**
 * 入口事件消息状态，用于区分可继续投递的正常消息与已终止处理的异常消息。
 */
public enum EventMessageStatus {

    /** 消息校验通过，可以创建并发布投递任务。 */
    NORMAL(1),

    /** 消息业务校验失败，仅保留原始消息和失败原因。 */
    ERROR(0);

    private final int code;

    EventMessageStatus(int code) {
        this.code = code;
    }

    /**
     * 获取数据库稳定存储值。
     *
     * @return 状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 根据数据库存储值恢复消息状态。
     *
     * @param code 状态码
     * @return 消息状态
     * @throws IllegalArgumentException 状态码无法识别
     */
    public static EventMessageStatus fromCode(int code) {
        for (EventMessageStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("无法识别的入口事件消息状态：" + code);
    }
}
