package org.etd.ai.knowledge.controller.vo;

import lombok.Data;

import java.time.Instant;

/** 知识库页面配置，模型绑定字段只读，实际检索尚未启用。 */
@Data
public class KnowledgeBaseVO {
    /** 知识库雪花标识。 */
    private Long id;

    /** 当前乐观锁版本，更新原样回传。 */
    private Long version;

    /** 启停状态：0停用、1启用，对应全局BasicConstant.DataStatus。 */
    private Integer dataStatus;

    /** 创建时间点。 */
    private Instant createTime;

    /** 更新时间点。 */
    private Instant updateTime;

    /** 知识库名称。 */
    private String knowledgeName;

    /** 知识库用途说明。 */
    private String description;

    /** 检索策略；基础阶段仅保存配置。 */
    private String retrievalMode;

    /** 可选模型配置标识。 */
    private Long embeddingModelConfigId;

    /** 配置维度，尚未实测核验。 */
    private Integer embeddingDimensions;

    /** 目标分片Token数。 */
    private Integer chunkMaxTokens;

    /** 分片重叠Token数。 */
    private Integer chunkOverlapTokens;

    /** 全文候选条数。 */
    private Integer keywordTopK;

    /** 向量候选条数。 */
    private Integer vectorTopK;

    /** 最终返回条数。 */
    private Integer retrievalTopK;

    /** 向量相似度阈值。 */
    private Double similarityThreshold;
}
