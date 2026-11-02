package org.etd.ai.agent.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;
import org.etd.ai.agent.constant.AgentPolicy;
import org.etd.framework.starter.ai.memory.semantic.AgentKnowledgeMode;

/** 草稿和发布共用的类型化配置；草稿允许缺少模型和提示词，发布由业务层完整校验。 */
@Data
public class AgentConfiguration {
    /** 已有聊天模型连接ID，草稿可不选，发布必填。 */
    @Positive
    private Long modelConnectionId;

    /** 系统提示词，草稿可空，发布要求非空白。 */
    @Size(max = AgentPolicy.MAX_PROMPT_LENGTH)
    private String systemPrompt;

    /** 通用模型生成参数，省略或字段为空表示不覆盖模型默认选项。 */
    @Valid
    private AgentModelOptions modelOptions;

    /** 知识库ID列表，允许为空列表或省略，不允许空元素、重复元素和跨租户引用。 */
    @Size(max = AgentPolicy.MAX_KNOWLEDGE_BASES)
    private List<@NotNull @Positive Long> knowledgeBaseIds;

    /** 本 Agent 可发现的官方工具名称；注册及用户权限必须分别校验，不接受模型扩大此范围。 */
    @Size(max = AgentPolicy.MAX_TOOLS)
    private List<@NotBlank @Size(max = AgentPolicy.MAX_TOOL_NAME_LENGTH) String> toolNames;

    /** 知识接入模式；省略保留已有预检索，tool 模式由官方 Agent 自主检索。 */
    private AgentKnowledgeMode knowledgeMode;

    /** 本 Agent 每次模型请求的输入 Token 预算，省略使用框架环境配置。 */
    @Positive
    private Integer inputTokenBudget;

    /** 本 Agent 单 Run 图内模型调用次数限制，不含检索词改写；省略使用框架环境配置。 */
    @Positive
    private Integer modelCallLimit;

    /** 本 Agent 单 Run 工具调用上限；达到后官方 Hook 在下一次模型调用前终止，省略使用框架环境配置。 */
    @Positive
    private Integer toolCallLimit;

    /** 历史消息条数上限，0表示不带历史；省略时由后续运行策略确定。 */
    @Min(0)
    @Max(AgentPolicy.MAX_HISTORY_MESSAGES)
    private Integer historyMessageLimit;

    /** 开场白，允许省略。 */
    @Size(max = AgentPolicy.MAX_OPENING_LENGTH)
    private String openingMessage;

    /** 推荐问题列表，允许省略；每项必须为非空白文本。 */
    @Size(max = AgentPolicy.MAX_SUGGESTED_QUESTIONS)
    private List<@NotBlank @Size(max = AgentPolicy.MAX_QUESTION_LENGTH) String> suggestedQuestions;

    /** 是否展示知识引用，省略时由后续运行策略确定，不影响检索权限检查。 */
    private Boolean citationEnabled;
}
