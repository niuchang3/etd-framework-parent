package org.etd.ai.agent.controller.vo;

import lombok.Data;
import java.time.Instant;
import java.util.List;
import org.etd.ai.agent.model.AgentCitation;

/** 消息历史响应，雪花ID按框架规则输出。 */
@Data
public class AgentChatMessageVO {
    /** 消息ID。 */
    private Long id;
    /** 所属运行ID。 */
    private Long runId;
    /** 会话消息序号。 */
    private Long sequenceNo;
    /** user或assistant。 */
    private String role;
    /** 已持久化正文。 */
    private String content;
    /** generating、completed、interrupted或failed。 */
    private String messageStatus;
    /** 引用来源，访问原资料仍需重新授权。 */
    private List<AgentCitation> citations;
    /** 创建时间。 */
    private Instant createTime;
}
