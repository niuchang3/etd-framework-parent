package org.etd.ai.knowledge.controller.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.Instant;

/** 知识库基本信息及检索设置，不接收生效索引和可信身份。 */
@Data
public class KnowledgeBaseSaveDTO {
    /** 知识库所属组织。 */
    @NotNull
    @Positive
    private Long orgId;

    /** 知识库启停状态：0禁用、1启用。 */
    @NotNull
    @Min(0)
    @Max(1)
    private Integer dataStatus;

    /** 乐观锁版本，更新时必须回传。 */
    @Min(0)
    private Long version;

    /** 知识库名称，租户内唯一。 */
    @NotBlank
    @Size(max = 100)
    private String name;

    /** 知识库简介。 */
    @Size(max = 1000)
    private String description;

    /** 是否租户内共享，并作为新文档公开默认值。 */
    @NotNull
    private Boolean isPublic;

    /** 检索模式：keyword、vector、hybrid。 */
    @NotNull
    @Pattern(regexp = "keyword|vector|hybrid")
    private String retrievalMode;

    /** 全文召回数量。 */
    @NotNull
    @Min(1)
    @Max(200)
    private Integer keywordRecallSize;

    /** 向量召回数量。 */
    @NotNull
    @Min(1)
    @Max(200)
    private Integer vectorRecallSize;

    /** 最终返回数量上限。 */
    @NotNull
    @Min(1)
    @Max(100)
    private Integer topK;

    /** 原始余弦相似度下限，空表示不过滤。 */
    @DecimalMin("-1")
    @DecimalMax("1")
    private Double similarityThreshold;

    /** 检索命中后的指针滑动上下文扩展窗口，0表示不扩展，1表示向前后各扩展1个相邻切片，上限为3。 */
    @Min(0)
    @Max(3)
    private Integer contextWindowSize;

    /** 是否启用重排。 */
    @NotNull
    private Boolean rerankEnabled;

    /** 重排模型连接ID，关闭时为空。 */
    @Positive
    private Long rerankConnectionId;

    /** 重排分数下限，空表示不限制。 */
    private Double rerankThreshold;

    /** 待构建的向量模型连接ID。 */
    @Positive
    private Long embeddingConnectionId;

    /** 待构建维度，仅768、1024或1536。 */
    @Min(1)
    private Integer dimensions;

}
