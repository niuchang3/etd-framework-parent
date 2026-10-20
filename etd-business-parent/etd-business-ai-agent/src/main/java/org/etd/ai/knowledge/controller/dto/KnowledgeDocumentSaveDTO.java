package org.etd.ai.knowledge.controller.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.etd.ai.knowledge.constant.KnowledgePolicy;

/** 资料配置编辑请求，不允许修改文件会话、归属部门及可信审计字段。 */
@Data
public class KnowledgeDocumentSaveDTO {
    /** 编辑所依据的乐观锁版本。 */
    @NotNull @Min(0)
    private Long version;

    /** 所属分类目录ID。 */
    @NotNull @Positive
    private Long folderId;

    /** 资料显示名称，最多255字。 */
    @NotBlank @Size(max = 255)
    private String documentName;

    /** 资料用途说明，最多1000字，可清空。 */
    @Size(max = 1000)
    private String description;

    /** 是否允许租户内公开检索，不授予维护权限。 */
    @NotNull
    private Boolean isPublic;

    /** 检索方式：keyword、vector、hybrid。 */
    @NotBlank @Pattern(regexp = KnowledgePolicy.RETRIEVAL_PATTERN)
    private String retrievalMode;

    /** 向量模型连接器ID，全文模式可不配置。 */
    @Positive
    private Long embeddingModelConnectionId;

    /** 向量维度，仅允许768、1024或1536。 */
    @Min(768) @Max(1536)
    private Integer embedding;

    /** 单个正文分片的Token上限。 */
    @NotNull @Min(1) @Max(KnowledgePolicy.MAX_CHUNK_TOKENS)
    private Integer chunkMaxTokens;

    /** 相邻正文分片的重叠Token数。 */
    @NotNull @Min(0)
    private Integer chunkOverlapTokens;

    /** 全文候选分片数量上限。 */
    @NotNull @Min(1) @Max(200)
    private Integer keywordTopK;

    /** 向量候选分片数量上限。 */
    @NotNull @Min(1) @Max(200)
    private Integer vectorTopK;

    /** 单份资料检索返回数量上限。 */
    @NotNull @Min(1) @Max(200)
    private Integer retrievalTopK;

    /** 向量相似度阈值，取值0至1，可为空。 */
    @DecimalMin("0") @DecimalMax("1")
    private Double similarityThreshold;

}
