package org.etd.framework.starter.ai.runtime.model;

import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.ai.chat.messages.Message;

/** 业务准备接口返回的聊天数据；只提供已授权事实，框架自行组装模型选项和执行流程。 */
@Data
public class AgentChatPreparation {
    /** 业务已建立并提交的本轮运行记录ID，后续保存回调使用同一标识。 */
    private String executionId;
    /** 授权模型连接实际所属租户，框架通过模型注册中心再次校验。 */
    private String modelTenantId;
    /** 本轮聊天模型连接ID，不传模型实例或凭证。 */
    private String modelConnectionId;
    /** 本轮系统提示词。 */
    private String systemPrompt;
    /** 已授权的历史消息，按时间升序排列，不含本轮问题。 */
    private List<Message> history = List.of();
    /** 本轮已授权绑定的知识库ID；空列表表示不调用检索接口。 */
    private List<Long> knowledgeBaseIds = List.of();
    /** 最终回答输出Token上限，未提供时使用框架配置。 */
    private Integer maxOutputTokens;
    /** 模型生成温度，未提供时沿用模型默认值。 */
    private Double temperature;
    /** 是否要求并展示知识引用，默认开启。 */
    private boolean citationsEnabled = true;
    /** 事件所需的业务关联标识，例如会话和消息ID；禁止存放凭证和内部配置。 */
    private Map<String, Object> attributes = Map.of();
}
