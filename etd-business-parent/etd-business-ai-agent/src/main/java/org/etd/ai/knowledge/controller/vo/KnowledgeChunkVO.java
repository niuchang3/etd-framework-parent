package org.etd.ai.knowledge.controller.vo;

import lombok.Data;
import lombok.ToString;
import org.etd.ai.knowledge.model.KnowledgeChunkSource;

/** 分片预览和编辑响应，只返回正文、顺序和来源。 */
@Data
@ToString(onlyExplicitlyIncluded = true)
public class KnowledgeChunkVO {
    /** 所属知识库ID。 */
    private Long knowledgeBaseId;

    /** 是否在当前租户内共享读取；分片继承所属资料。 */
    private Boolean isPublic;
    /** 当前角色数据范围是否允许维护；还须具备接口write权限。 */
    private Boolean writable;
    /** 分片ID。 */
    private Long id;
    /** 资料ID。 */
    private Long documentId;
    /** 原始排序号，从0开始，合并后允许空号；页面按返回顺序显示。 */
    private Integer chunkIndex;
    /** 可编辑的权威正文。 */
    private String content;
    /** cl100k_base正文Token数。 */
    private Integer tokenCount;
    /** 页码、标题和结构块引用。 */
    private KnowledgeChunkSource source;
}
