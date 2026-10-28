package org.etd.ai.agent.controller.dto;

import lombok.Data;
import org.etd.ai.agent.constant.AgentChatPolicy;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 新会话输入，不接受租户和所有者字段。 */
@Data
public class AgentChatSessionDTO {
    /** Agent ID，必须可使用。 */
    @NotNull @Positive
    private Long agentId;
    /** chat正式聊天或debug调试，必填。 */
    @NotBlank @Pattern(regexp = AgentChatPolicy.CONVERSATION_TYPE_PATTERN)
    private String conversationType;
    /** 会话标题，非空且最多200字。 */
    @NotBlank @Size(max = 200)
    private String title;
}
