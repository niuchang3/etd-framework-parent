package org.etd.ai.knowledge.controller.vo;

import lombok.Data;

/** 资料配置可选择的已启用向量连接器，不依赖模型广场菜单权限。 */
@Data
public class KnowledgeEmbeddingOptionVO {
    /** 向量连接器ID，保存到资料的embeddingModelConnectionId。 */
    private Long id;
    /** 平台模型ID，仅供展示，保存时由后端确认。 */
    private Long modelId;
    /** 模型显示名称。 */
    private String modelName;
    /** 服务商字典值。 */
    private String providerCode;
    /** 当前模型允许选择的向量维度集合。 */
    private Integer[] embedding;
}
