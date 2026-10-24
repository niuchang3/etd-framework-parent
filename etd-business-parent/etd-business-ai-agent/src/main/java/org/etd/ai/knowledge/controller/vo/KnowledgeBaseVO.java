package org.etd.ai.knowledge.controller.vo;

import lombok.Data;
import java.time.Instant;

/** 知识库卡片与设置详情。 */
@Data
public class KnowledgeBaseVO {
    /** 知识库ID。 */
    private Long id;

    /** 组织归属。 */
    private Long orgId;

    /** 启停状态。 */
    private Integer dataStatus;

    /** 创建时间。 */
    private Instant createTime;

    /** 乐观锁版本，更新时必须回传。 */
    private Long version;

    /** 知识库名称，租户内唯一。 */
    private String name;

    /** 知识库简介。 */
    private String description;

    /** 是否租户内共享，并作为新文档公开默认值。 */
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
    private Double similarityThreshold;

    /** 是否启用重排。 */
    private Boolean rerankEnabled;

    /** 重排模型连接ID，关闭时为空。 */
    private Long rerankConnectionId;

    /** 重排分数下限，空表示不限制。 */
    private Double rerankThreshold;

    /** 待构建的向量模型连接ID。 */
    private Long embeddingConnectionId;

    /** 待构建维度，仅768、1024或1536。 */
    private Integer dimensions;

    /** 正式生效索引使用的模型连接ID。 */
    private Long activeConnectionId;

    /** 正式生效索引维度。 */
    private Integer activeDimensions;

    /** 生效向量索引代次，0表示尚未初始化。 */
    private Long activeIndexVersion;

    /** 正在构建或最近失败的索引代次。 */
    private Long buildingIndexVersion;

    /** idle、building、ready或failed。 */
    private String indexStatus;

    /** 本次需要处理的分片数。 */
    private Integer indexTotal;

    /** 本次已完成分片数。 */
    private Integer indexCompleted;

    /** 脱敏后的索引错误。 */
    private String indexError;

    /** 本次构建开始时间，用于超时恢复。 */
    private Instant indexStartedTime;

    /** 待构建向量模型展示名称。 */
    private String embeddingModelName;

    /** 当前生效向量模型展示名称。 */
    private String activeModelName;

    /** 当前用户可读文档数量。 */
    private Long documentCount;

    /** 知识库数据范围是否允许维护。 */
    private Boolean writable;

}
