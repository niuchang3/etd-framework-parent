package org.etd.ai.agent.controller.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.etd.ai.agent.constant.AgentChatPolicy;

/** 流式问答输入；重复requestId不再次调用模型。 */
@Data
public class AgentChatMessageDTO {
    /** 客户端生成的会话内唯一标识，最多64字符；重试同一次提交应保持不变。 */
    @NotBlank @Size(max = 64)
    private String requestId;
    /** 本轮用户问题，最多2000字符。 */
    @NotBlank @Size(max = AgentChatPolicy.MAX_QUESTION_CHARACTERS)
    private String content;
}
