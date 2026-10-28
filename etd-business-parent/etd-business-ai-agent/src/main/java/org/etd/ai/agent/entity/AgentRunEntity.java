package org.etd.ai.agent.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.Instant;

/** 单轮执行记录；不保存Token凭证或运行上下文。 */
@Data
@TableName("ai_agent_run")
public class AgentRunEntity {
    /** 运行雪花ID。 */
    @TableId
    private Long id;
    /** 所属租户。 */
    private Long tenantId;
    /** 所属会话。 */
    private Long conversationId;
    /** 会话内请求幂等标识。 */
    private String requestId;
    /** 本轮模型连接。 */
    private Long modelConnectionId;
    /** 执行状态，遵循AgentChatPolicy。 */
    private String runStatus;
    /** 执行开始时间点。 */
    private Instant startedTime;
    /** 终态时间点。 */
    private Instant finishedTime;
    /** 运行租约到期时间点。 */
    private Instant leaseUntil;
    /** 服务商返回的输入用量，未知为空。 */
    private Long inputTokens;
    /** 服务商返回的输出用量，未知为空。 */
    private Long outputTokens;
    /** 稳定错误码。 */
    private String errorCode;
    /** 脱敏错误说明。 */
    private String errorMessage;
}
