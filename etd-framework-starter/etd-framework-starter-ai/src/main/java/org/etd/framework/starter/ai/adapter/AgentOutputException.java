package org.etd.framework.starter.ai.adapter;

/**
 * 输出通道断开异常：当客户端提前关闭连接或网络中断时触发，用于立即通知执行引擎取消任务。
 */
public final class AgentOutputException extends RuntimeException {

    public AgentOutputException() {
        super("客户端输出通道已关闭");
    }

    public AgentOutputException(String message) {
        super(message);
    }
}
