package org.etd.ai.agent.controller.vo;

import lombok.Data;
import java.util.List;

/** 聊天页面展示配置；不暴露系统提示词、连接标识或知识库绑定。 */
@Data
public class AgentChatProfileVO {
    /** Agent ID。 */
    private Long agentId;
    /** Agent名称。 */
    private String name;
    /** Agent简介。 */
    private String description;
    /** 头像资源路径。 */
    private String avatarPath;
    /** 开场白，仅供界面展示，不自动写入历史。 */
    private String openingMessage;
    /** 推荐问题，点击后按普通问题提交。 */
    private List<String> suggestedQuestions;
    /** 是否展示知识引用。 */
    private Boolean citationEnabled;
}
