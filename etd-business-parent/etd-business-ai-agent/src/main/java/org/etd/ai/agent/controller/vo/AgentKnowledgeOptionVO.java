package org.etd.ai.agent.controller.vo;

import lombok.Data;

/** Agent知识库选择项，仅返回当前可读且启用的知识库基本信息。 */
@Data
public class AgentKnowledgeOptionVO {
    /** 知识库ID，写入draftConfig.knowledgeBaseIds。 */
    private Long id;
    /** 知识库名称。 */
    private String name;
    /** 知识库简介。 */
    private String description;
    /** 所属组织ID。 */
    private Long orgId;
    /** 是否租户内共享；不表示其中全部资料都可访问。 */
    private Boolean isPublic;
}
