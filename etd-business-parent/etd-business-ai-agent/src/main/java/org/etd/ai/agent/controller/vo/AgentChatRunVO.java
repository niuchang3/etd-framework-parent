package org.etd.ai.agent.controller.vo;

import lombok.Data;
import java.time.Instant;

/** 运行状态响应，用于断线后查询，不重放SSE。 */
@Data
public class AgentChatRunVO {
    /** Run ID。 */
    private Long id;
    /** 会话ID。 */
    private Long conversationId;
    /** 请求幂等标识。 */
    private String requestId;
    /** running、cancel_requested、succeeded、failed或cancelled。 */
    private String runStatus;
    /** 开始时间。 */
    private Instant startedTime;
    /** 结束时间。 */
    private Instant finishedTime;
    /** 已知输入Token数。 */
    private Long inputTokens;
    /** 已知输出Token数。 */
    private Long outputTokens;
    /** 失败错误码。 */
    private String errorCode;
    /** 脱敏错误说明。 */
    private String errorMessage;
}
