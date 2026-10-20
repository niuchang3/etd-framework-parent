package org.etd.ai.knowledge.controller.vo;

import lombok.Data;

/** 当前轮次正文分片预览。 */
@Data
public class KnowledgeChunkVO {
    /** 正文分片ID。 */
    private Long id;
    /** 所属资料ID。 */
    private Long documentId;
    /** 处理轮次。 */
    private Long processingVersion;
    /** 分片顺序，从0开始。 */
    private Integer chunkIndex;
    /** 分片标题。 */
    private String title;
    /** 分片正文，前端以文本方式渲染。 */
    private String content;
    /** 正文Token数量。 */
    private Integer tokenCount;
    /** 向量处理状态。 */
    private String embeddingStatus;
}
