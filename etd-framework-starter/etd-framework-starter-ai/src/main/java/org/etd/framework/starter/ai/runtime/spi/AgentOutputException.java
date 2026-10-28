package org.etd.framework.starter.ai.runtime.spi;

/** 输出通道断开信号；不包含原始网络异常、请求头或认证信息。 */
public final class AgentOutputException extends RuntimeException {
    /** 创建统一的接收端断开信号。 */
    public AgentOutputException() {
        super("运行输出通道已断开");
    }
}
