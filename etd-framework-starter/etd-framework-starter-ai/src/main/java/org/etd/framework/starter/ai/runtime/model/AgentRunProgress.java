package org.etd.framework.starter.ai.runtime.model;

import java.util.List;

/** Agent执行进度，记录当前回答、引用资料及Token用量，不包含请求头或凭证。
 * @param executionId 业务执行标识
 * @param content 已生成正文
 * @param references 实际纳入上下文且允许展示的来源
 * @param inputTokens 已知输入用量，未知为空
 * @param outputTokens 已知输出用量，未知为空
 */
public record AgentRunProgress(String executionId, String content, List<KnowledgeFragment> references,
        Long inputTokens, Long outputTokens) {
}
