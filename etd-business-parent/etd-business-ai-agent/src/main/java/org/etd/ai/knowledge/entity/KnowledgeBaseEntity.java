package org.etd.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.etd.framework.starter.mybaits.core.BaseEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;

/** KnowledgeBaseEntity 持久化记录；租户及审计字段由框架可信上下文填充。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_knowledge_base")
public class KnowledgeBaseEntity extends BaseEntity {
    /** 非负乐观锁版本，修改时必须提交当前值。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    @Version
    private Long version;

    /** 知识库名称。 */
    private String knowledgeName;

    /** 知识库用途说明。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
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
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Double similarityThreshold;
}
