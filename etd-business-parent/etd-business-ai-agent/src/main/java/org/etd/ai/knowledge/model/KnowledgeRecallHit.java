package org.etd.ai.knowledge.model;

import lombok.Data;

/** 召回候选及最终结果；正文与权限始终通过当前资料关联读取。 */
@Data
public class KnowledgeRecallHit {
    /** 命中分片ID。 */
    private Long chunkId;
    /** 所属资料ID。 */
    private Long documentId;
    /** 资料显示名称。 */
    private String documentName;
    /** 分片原文顺序，从0开始。 */
    private Integer chunkIndex;
    /** 当前分片正文。 */
    private String content;
    /** 原文标题、页码及块引用。 */
    private KnowledgeChunkSource source;
    /** 查询时资料内容轮次，用于模型调用后复核。 */
    private Long processingVersion;
    /** 全文排名分数，未参与全文召回时为空。 */
    private Double keywordScore;
    /** 原始余弦相似度，取值[-1,1]，未参与向量召回时为空。 */
    private Double vectorScore;
    /** 模型重排相关性分数，未重排时为空。 */
    private Double rerankScore;
    /** 最终排序分数：单路原始分数、混合RRF分数或重排分数。 */
    private Double score;
}
