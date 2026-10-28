package org.etd.ai.agent.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableField;
import org.etd.ai.agent.mapper.typehandler.AgentCitationTypeHandler;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.Instant;
import java.util.List;
import org.etd.ai.agent.model.AgentCitation;

/** 每轮一问一答；引用仅保存来源，不重复保存检索正文。 */
@Data
@TableName("ai_agent_message")
public class AgentMessageEntity {
    /** 消息雪花ID。 */
    @TableId
    private Long id;
    /** 所属租户。 */
    private Long tenantId;
    /** 所属会话。 */
    private Long conversationId;
    /** 所属运行ID。 */
    private Long runId;
    /** 会话内递增消息序号。 */
    private Long sequenceNo;
    /** user用户或assistant助手。 */
    private String role;
    /** 正文，中断时保留已生成内容。 */
    private String content;
    /** 消息状态，遵循AgentChatPolicy。 */
    private String messageStatus;
    /** 实际纳入上下文的引用，关闭展示时为空列表。 */
    @TableField(typeHandler = AgentCitationTypeHandler.class)
    private List<AgentCitation> citations;
    /** 消息创建时间点。 */
    private Instant createTime;
    /** 消息更新时间点。 */
    private Instant updateTime;
}
