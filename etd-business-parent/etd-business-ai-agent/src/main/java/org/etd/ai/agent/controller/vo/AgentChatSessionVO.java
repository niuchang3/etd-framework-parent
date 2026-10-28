package org.etd.ai.agent.controller.vo;

import lombok.Data;
import java.time.Instant;

/** 聊天会话响应，不暴露Agent内部指令。 */
@Data
public class AgentChatSessionVO {
    /** 会话ID。 */
    private Long id;
    /** Agent ID。 */
    private Long agentId;
    /** 会话标题。 */
    private String title;
    /** chat或debug。 */
    private String conversationType;
    /** 最近消息时间。 */
    private Instant lastMessageTime;
    /** 创建时间。 */
    private Instant createTime;
}
