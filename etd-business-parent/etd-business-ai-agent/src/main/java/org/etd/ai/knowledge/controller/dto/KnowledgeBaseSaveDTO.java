package org.etd.ai.knowledge.controller.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

import org.etd.ai.knowledge.constant.AiStage;
import org.etd.framework.starter.web.validation.UpdateRequest;

/** KnowledgeBaseSaveDTO 页面请求；不接收租户、创建人或存储对象键。 */
@Data
public class KnowledgeBaseSaveDTO {
    /** 知识库名称，最多100字。 */
    @NotBlank
    @Size(max = 100)
    private String knowledgeName;

    /** 用途说明，最多1000字。 */
    @Size(max = 1000)
    private String description;

    /** keyword、vector、hybrid；基础阶段仅保存草稿策略。 */
    @NotBlank
    @Pattern(regexp = "keyword|vector|hybrid")
    private String retrievalMode;

    /** 目标Token数，默认512。 */
    @Min(1)
    @Max(32768)
    private Integer chunkMaxTokens = AiStage.CHUNK_SIZE;

    /** 重叠Token数，默认64。 */
    @Min(0)
    @Max(32767)
    private Integer chunkOverlapTokens = AiStage.OVERLAP;

    /** 全文候选条数，默认20。 */
    @Min(1)
    @Max(200)
    private Integer keywordTopK = AiStage.CANDIDATES;

    /** 向量候选条数，默认20。 */
    @Min(1)
    @Max(200)
    private Integer vectorTopK = AiStage.CANDIDATES;

    /** 最终条数，默认5。 */
    @Min(1)
    @Max(200)
    private Integer retrievalTopK = AiStage.RESULTS;

    /** 可选相似度0至1。 */
    @DecimalMin("0")
    @DecimalMax("1")
    private Double similarityThreshold;

    /** 更新必填，新增省略。 */
    @Min(0)
    @NotNull(groups = UpdateRequest.class)
    private Long version;
}
