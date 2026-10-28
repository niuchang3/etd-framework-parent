package org.etd.framework.starter.ai.runtime.model;

/** 与传输协议无关的运行事件；HTTP事件名称由输出插槽映射。 */
public enum AgentRuntimeEventType {
    /** 执行开始。 */
    START,
    /** 实际输入引用已确定。 */
    REFERENCES,
    /** 新增文本。 */
    DELTA,
    /** 输出通道保活。 */
    HEARTBEAT,
    /** 持久化完成后的实际终态。 */
    TERMINAL
}
