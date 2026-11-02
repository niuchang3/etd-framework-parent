package org.etd.framework.starter.ai.runtime.model;

/** Web 入口提交的数据，不接受客户端指定的可信身份。
 * @param sessionId 个人会话标识
 * @param requestId 本轮幂等标识
 * @param question 用户提问
 */
public record AgentChatCommand(Long sessionId, String requestId, String question) {
}
