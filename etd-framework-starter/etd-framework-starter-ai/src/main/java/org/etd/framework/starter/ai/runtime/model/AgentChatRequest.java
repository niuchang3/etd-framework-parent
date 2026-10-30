package org.etd.framework.starter.ai.runtime.model;

import org.springframework.util.Assert;

/** 调用方提交的一轮聊天参数，不包含执行器、回调组装或内部运行策略。
 * @param sessionId 已有会话ID，所有权由聊天准备接口校验
 * @param requestId 本轮幂等标识，同一会话内不得重复执行
 * @param question 本轮用户问题
 */
public record AgentChatRequest(Long sessionId, String requestId, String question) {
    public AgentChatRequest {
        Assert.notNull(sessionId, "会话ID不能为空");
        Assert.hasText(requestId, "请求标识不能为空");
        Assert.hasText(question, "用户问题不能为空");
        question = question.trim();
    }
}
