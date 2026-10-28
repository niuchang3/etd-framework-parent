package org.etd.ai.agent.model;

import lombok.Data;
import org.etd.ai.knowledge.model.KnowledgeChunkSource;

/** 实际提供给模型的引用来源；原文下载必须重新授权。 */
@Data
public class AgentCitation {
    /** 从1开始的引用编号。 */
    private Integer number;
    /** 知识库ID。 */
    private Long knowledgeBaseId;
    /** 资料ID。 */
    private Long documentId;
    /** 中心分片ID。 */
    private Long chunkId;
    /** 资料展示名称。 */
    private String documentName;
    /** 来源页码、标题及结构位置。 */
    private KnowledgeChunkSource source;
}
