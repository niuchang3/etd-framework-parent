package org.etd.framework.starter.ai.runtime.model;

import java.util.List;
import java.util.function.Supplier;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;

/** 框架内部的模型运行输入，由统一聊天入口构造，业务不直接组装。
 * @param systemPrompt Agent系统指令
 * @param question 本轮问题
 * @param history 已授权的完整历史问答
 * @param options 本轮生成选项
 * @param knowledgeSupplier 框架组装的本轮知识调用，不由业务构造
 * @param inputTokenBudget 输入Token预算，cl100k估算，不声明为供应商精确值
 * @param maxReferences 整轮引用上限
 * @param citationsEnabled 是否向模型要求引用编号
 */
public record AgentExecutionRequest(String systemPrompt, String question, List<Message> history,
        ChatOptions options, Supplier<List<List<KnowledgeFragment>>> knowledgeSupplier, int inputTokenBudget,
        int maxReferences, boolean citationsEnabled) {
}
