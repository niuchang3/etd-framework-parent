package org.etd.ai.agent.model;

import java.util.List;
import org.etd.ai.agent.entity.AgentRunEntity;
import org.springframework.ai.chat.messages.Message;

/** 短事务准备好的本轮输入，不写入数据库配置快照。
 * @param run 运行记录
 * @param assistantMessageId 助手消息ID
 * @param configuration 本轮独立配置
 * @param history 完整成功问答历史
 * @param question 用户问题
 * @param modelTenantId 模型连接的实际所属租户
 */
public record AgentChatExecution(AgentRunEntity run, Long assistantMessageId, AgentConfiguration configuration,
        List<Message> history, String question, String modelTenantId) {
}
