package org.etd.framework.starter.ai.runtime.model;

import java.util.List;
import java.util.Map;
import org.etd.framework.starter.ai.memory.semantic.AgentKnowledgeMode;
import org.springframework.ai.chat.messages.Message;

/** 业务短事务返回的授权数据，不含模型客户端、执行器、回调或凭证。
 * @param runId 已创建的 Run 标识
 * @param modelTenantId 模型连接所属租户，仍须由网关再次授权
 * @param modelConnectionId 已授权连接标识
 * @param systemPrompt 已发布或有调试资格的系统指令
 * @param history 已授权历史消息
 * @param maxOutputTokens 可选输出 Token 上限
 * @param temperature 可选生成温度
 * @param knowledgeBaseIds Agent 已绑定的知识库范围，实际检索必须按当前用户独立授权
 * @param toolNames Agent 配置允许的官方工具名称，不代表当前用户有执行权限
 * @param citationsEnabled 是否要求模型输出引用编号
 * @param knowledgeMode 知识接入模式，省略时有绑定则预检索，否则纯模型
 * @param inputTokenBudget 可选的 Agent 输入预算，仍以 Token 而非消息条数控制
 * @param modelCallLimit 可选的单 Run 图内模型调用上限，不含检索词改写
 * @param toolCallLimit 可选的单 Run 工具调用次数上限
 * @param attributes SSE 关联数据，不包含可信身份
 */
public record AgentChatPreparation(String runId, String modelTenantId, String modelConnectionId,
        String systemPrompt, List<Message> history, Integer maxOutputTokens, Double temperature,
        List<Long> knowledgeBaseIds, List<String> toolNames, boolean citationsEnabled, AgentKnowledgeMode knowledgeMode,
        Integer inputTokenBudget, Integer modelCallLimit,
        Integer toolCallLimit, Map<String, Object> attributes) {
    public AgentChatPreparation {
        history = List.copyOf(history);
        knowledgeBaseIds = knowledgeBaseIds == null ? List.of() : List.copyOf(knowledgeBaseIds);
        toolNames = toolNames == null ? List.of() : List.copyOf(toolNames);
        attributes = Map.copyOf(attributes);
    }
}
