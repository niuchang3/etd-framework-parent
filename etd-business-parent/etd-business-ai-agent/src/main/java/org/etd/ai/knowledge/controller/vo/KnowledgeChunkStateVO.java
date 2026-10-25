package org.etd.ai.knowledge.controller.vo;

import java.time.Instant;
import lombok.Data;
import org.etd.framework.starter.ai.document.model.DocumentChunkParameters;

/** 分片工作页状态；所有写操作使用这里返回的资料version。 */
@Data
public class KnowledgeChunkStateVO {
    /** 所属知识库ID。 */
    private Long knowledgeBaseId;

    /** 是否在当前租户内共享读取；分片继承所属资料。 */
    private Boolean isPublic;
    /** 当前角色数据范围是否允许维护；还须具备接口write权限。 */
    private Boolean writable;
    /** 资料ID。 */
    private Long id;
    /** 资料显示名称。 */
    private String documentName;
    /** 原始文件名，用于识别格式。 */
    private String originalFilename;
    /** 文件类型。 */
    private String fileType;
    /** 资料乐观锁版本。 */
    private Long version;
    /** 分片状态。 */
    private String chunkStatus;
    /** 独立向量状态，取值对应ai_knowledge_vector_status字典，由后端维护。 */
    private String vectorStatus;
    /** 已保存分片数量。 */
    private Integer chunkCount;
    /** 最近成功生成使用的参数，尚未生成时为空。 */
    private DocumentChunkParameters chunkParameters;
    /** 最近一次生成失败摘要。 */
    private String chunkErrorMessage;
    /** 已确认结果的时间。 */
    private Instant chunkConfirmedTime;
    /** 当前生成截止时间，超过此时间允许人工重新生成。 */
    private Instant chunkGenerationExpiresAt;
    /** 分片正文Token计数使用的分词器，不等同于原生生成预算所选分词器。 */
    private String tokenCountTokenizer;
}
