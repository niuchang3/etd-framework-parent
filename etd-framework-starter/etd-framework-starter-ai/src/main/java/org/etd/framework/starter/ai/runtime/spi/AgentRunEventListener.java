package org.etd.framework.starter.ai.runtime.spi;

import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEvent;

/** 输出插槽，可由SSE、WebSocket或其他调用方实现，不绑定Servlet。 */
public interface AgentRunEventListener {
    /** @param event 按序输出的运行事件 @throws AgentOutputException 接收端断开，框架将取消上游 */
    void onEvent(AgentRuntimeEvent event);
    /** 执行结束释放输出通道；实现方必须可重复调用且不得抛出异常。 */
    void close();
}
