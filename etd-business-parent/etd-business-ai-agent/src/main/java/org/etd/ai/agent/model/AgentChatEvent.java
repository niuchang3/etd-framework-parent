package org.etd.ai.agent.model;

import java.util.List;
import lombok.Data;

/** 统一SSE事件数据；每条事件都带标识，前端不依赖供应商原始响应。 */
@Data
public class AgentChatEvent {
    /** 会话ID。 */
    private Long sessionId;
    /** 本轮执行ID，停止及状态查询使用。 */
    private Long runId;
    /** 助手消息ID。 */
    private Long messageId;
    /** 同一流内递增事件序号，不支持断点重放。 */
    private Long sequence;
    /** delta事件的新增文本，不能替换已有正文。 */
    private String text;
    /** references事件的实际上下文引用。 */
    private List<AgentCitation> references;
    /** done/error事件的运行终态。 */
    private String runStatus;
    /** 已知输入Token用量，未知为空。 */
    private Long inputTokens;
    /** 已知输出Token用量，未知为空。 */
    private Long outputTokens;
    /** 失败错误码。 */
    private String errorCode;
    /** 脱敏错误说明。 */
    private String errorMessage;
}
