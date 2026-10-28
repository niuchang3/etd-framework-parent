package org.etd.framework.starter.ai.runtime.model;

import java.util.List;
import org.etd.framework.starter.ai.runtime.spi.AgentKnowledgeQueryService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;

/** 单轮运行输入，与HTTP、业务实体及持久化解耦。
 * @param systemPrompt Agent系统指令
 * @param question 本轮问题
 * @param history 已授权的完整历史问答
 * @param options 本轮生成选项
 * @param knowledgeService 本轮授权的知识检索扩展
 * @param inputTokenBudget 输入Token预算，cl100k估算，不声明为供应商精确值
 * @param maxReferences 整轮引用上限
 * @param citationsEnabled 是否向模型要求引用编号
 */
public record AgentExecutionRequest(String systemPrompt, String question, List<Message> history,
        ChatOptions options, AgentKnowledgeQueryService knowledgeService, int inputTokenBudget,
        int maxReferences, boolean citationsEnabled) {
}
