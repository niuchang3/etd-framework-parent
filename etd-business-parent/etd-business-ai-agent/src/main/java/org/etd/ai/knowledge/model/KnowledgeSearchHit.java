package org.etd.ai.knowledge.model;

import lombok.Data;
import org.etd.ai.knowledge.model.KnowledgeChunkSource;

/** 检索候选内部模型，Mapper不直接返回Controller VO。 */
@Data
public class KnowledgeSearchHit {
    /** 命中分片ID。 */
    private Long chunkId;

    /** 所属文档ID。 */
    private Long documentId;

    /** 文档名称。 */
    private String documentName;

    /** 当前权威正文。 */
    private String content;

    /** 来源标题、页码和结构块引用。 */
    private KnowledgeChunkSource source;

    /** 文档启停状态。 */
    private Integer dataStatus;

    /** 全文原始得分。 */
    private Double keywordScore;

    /** 原始余弦相似度。 */
    private Double vectorScore;

    /** 按排名融合得分。 */
    private Double fusionScore;

    /** 重排原始得分。 */
    private Double rerankScore;

    /** 文档正文轮次。 */
    private Long processingVersion;

}
