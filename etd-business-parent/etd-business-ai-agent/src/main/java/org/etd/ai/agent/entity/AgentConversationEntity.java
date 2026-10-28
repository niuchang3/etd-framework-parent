package org.etd.ai.agent.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.Instant;

/** 个人聊天会话；归属校验由会话Service执行，不随Agent公开。 */
@Data
@TableName("ai_agent_conversation")
public class AgentConversationEntity {
    /** 会话雪花ID。 */
    @TableId
    private Long id;
    /** 会话所属租户。 */
    private Long tenantId;
    /** 绑定的Agent ID。 */
    private Long agentId;
    /** 会话所有者ID。 */
    private Long userId;
    /** 会话标题，最多200字。 */
    private String title;
    /** chat正式会话或debug调试会话。 */
    private String conversationType;
    /** 最近消息时间点。 */
    private Instant lastMessageTime;
    /** 会话创建时间点。 */
    private Instant createTime;
    /** 会话更新时间点。 */
    private Instant updateTime;
}
