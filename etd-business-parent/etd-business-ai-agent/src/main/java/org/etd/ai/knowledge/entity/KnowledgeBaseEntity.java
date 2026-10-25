package org.etd.ai.knowledge.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.OrgScopedEntity;
import com.baomidou.mybatisplus.annotation.*;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;
import java.time.Instant;

/** 知识库配置及当前、待生效索引状态；凭证由模型连接管理。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_knowledge_base")
public class KnowledgeBaseEntity extends OrgScopedEntity {
    /** 乐观锁版本，更新时必须回传。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    @Version
    private Long version;

    /** 知识库名称，租户内唯一。 */
    private String name;

    /** 知识库简介。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    /** 是否租户内共享，并作为新文档公开默认值。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("false")
    private Boolean isPublic;

    /** 检索模式：keyword、vector、hybrid。 */
    private String retrievalMode;

    /** 全文召回数量。 */
    private Integer keywordRecallSize;

    /** 向量召回数量。 */
    private Integer vectorRecallSize;

    /** 最终返回数量上限。 */
    private Integer topK;

    /** 原始余弦相似度下限，空表示不过滤。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Double similarityThreshold;

    /** 是否启用重排。 */
    private Boolean rerankEnabled;

    /** 重排模型连接ID，关闭时为空。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long rerankConnectionId;

    /** 重排分数下限，空表示不限制。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Double rerankThreshold;

    /** 待构建的向量模型连接ID。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long embeddingConnectionId;

    /** 待构建维度，仅768、1024或1536。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer dimensions;

    /** 正式生效索引使用的模型连接ID。 */
    private Long activeConnectionId;

    /** 正式生效索引维度。 */
    private Integer activeDimensions;

    /** 生效向量索引代次，0表示尚未初始化。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    private Long activeIndexVersion;

    /** 正在构建或最近失败的索引代次。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    private Long buildingIndexVersion;

    /** idle、building、ready或failed。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("'idle'")
    private String indexStatus;

    /** 本次需要处理的分片数。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    private Integer indexTotal;

    /** 本次已完成分片数。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    private Integer indexCompleted;

    /** 脱敏后的索引错误。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String indexError;

    /** 本次构建开始时间，用于超时恢复。 */
    private Instant indexStartedTime;

    /** 后台工作租约到期时间，不由客户端指定。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Instant indexLeaseUntil;

    /** 配置的向量模型展示名称。 */
    @TableField(exist = false)
    private String embeddingModelName;

    /** 当前用户可读文档数量。 */
    @TableField(exist = false)
    private Long documentCount;

    /** 知识库数据范围是否允许维护。 */
    @TableField(exist = false)
    private Boolean writable;

}
