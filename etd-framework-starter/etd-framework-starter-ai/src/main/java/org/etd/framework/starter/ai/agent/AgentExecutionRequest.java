package org.etd.framework.starter.ai.agent;

import org.springframework.ai.document.Document;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;

/** 执行适配内部的模型输入数据；业务只返回授权配置，不组装此对象。
 * @param systemPrompt Agent系统指令
 * @param question 本轮问题
 * @param history 已授权的完整历史问答
 * @param options 本轮生成选项
 * @param knowledgeGroups 已完成授权检索的候选数据；不携带待执行流程
 * @param inputTokenBudget 输入Token预算，cl100k估算，不声明为供应商精确值
 * @param maxReferences 整轮引用上限
 * @param citationsEnabled 是否向模型要求引用编号
 */
public record AgentExecutionRequest(String systemPrompt, String question, List<Message> history,
        ChatOptions options, List<List<Document>> knowledgeGroups, int inputTokenBudget,
        int maxReferences, boolean citationsEnabled) {
}
