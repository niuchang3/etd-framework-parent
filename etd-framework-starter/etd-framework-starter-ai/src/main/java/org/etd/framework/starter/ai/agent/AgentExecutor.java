package org.etd.framework.starter.ai.agent;

import org.etd.framework.starter.ai.runtime.AgentRunControl;
import org.etd.framework.starter.ai.runtime.model.AgentChatPreparation;
import reactor.core.publisher.Flux;

/**
 * 智能体执行中枢顶级接口：负责协调模型、记忆与工具调用。
 * 复用官方 ReactAgent，只向运行容器输出正文增量。
 */
public interface AgentExecutor {

    /**
     * 执行智能体推理流。
     * @param preparation 已授权的 Agent 配置与历史数据
     * @param question 当前用户问题
     * @param control 本次运行的中断与生命周期控制句柄
     * @return 可立即输出的正文增量
     */
    Flux<String> execute(AgentChatPreparation preparation, String question, AgentRunControl control);
}
