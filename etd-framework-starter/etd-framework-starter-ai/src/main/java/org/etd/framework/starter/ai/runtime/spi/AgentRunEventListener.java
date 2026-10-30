package org.etd.framework.starter.ai.runtime.spi;

import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEvent;
import org.etd.framework.starter.ai.runtime.AgentRunControl;

/** 输出插槽，可由SSE、WebSocket或其他调用方实现，不绑定Servlet。 */
public interface AgentRunEventListener {
    /** 框架启动后绑定本轮控制器；HTTP输出实现可据此在断线时取消，普通事件接收器无需实现。
     * 绑定前可能已收到事件，实现方必须处理接收端先断开的情况；绑定失败时框架会请求取消。
     * @param control 本轮取消入口，不负责持久化或线程管理
     */
    default void bind(AgentRunControl control) {
    }
    /** @param event 按序输出的运行事件 @throws AgentOutputException 接收端断开，框架将取消上游 */
    void onEvent(AgentRuntimeEvent event);
    /** 执行结束释放输出通道；实现方必须可重复调用且不得抛出异常。 */
    void close();
}
