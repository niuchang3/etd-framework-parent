package org.etd.ai.agent.controller.vo;

import lombok.Data;

/** Agent模型选择项，只返回已启用的聊天模型连接，不暴露服务地址或凭证。 */
@Data
public class AgentModelOptionVO {
    /** 连接ID，写入draftConfig.modelConnectionId。 */
    private Long id;
    /** 平台模型ID。 */
    private Long modelId;
    /** 模型展示名称。 */
    private String modelName;
    /** 服务商编码。 */
    private String providerCode;
    /** 模型Logo资源路径。 */
    private String logoPath;
}
