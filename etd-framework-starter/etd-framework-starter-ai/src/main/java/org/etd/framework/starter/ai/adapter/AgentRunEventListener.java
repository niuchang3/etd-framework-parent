package org.etd.framework.starter.ai.adapter;

import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEvent;

/**
 * 统一执行事件监听器：上层协议适配器（如SSE/WebSocket）通过此接口接收模型与工具事件。
 */
public interface AgentRunEventListener {

    /**
     * 接收到运行事件（包含思考、动作、观察、增量正文、终态等）。
     * @param event 统一事件
     */
    void onEvent(AgentRuntimeEvent event);

    /**
     * 运行通道关闭通知。
     */
    default void close() {
    }
}
