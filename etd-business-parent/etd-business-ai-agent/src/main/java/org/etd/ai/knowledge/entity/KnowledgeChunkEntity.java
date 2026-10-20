package org.etd.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.OrgScopedEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;
import java.time.Instant;

/** 正文分片实体，始终与资料当前处理轮次及权限副本保持一致。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_knowledge_chunk")
public class KnowledgeChunkEntity extends OrgScopedEntity {
    /** 分片乐观锁版本。 */
    @Version @TableField(fill = FieldFill.INSERT) @TableFieldExtend("0")
    private Long version;
    /** 所属资料ID。 */
    private Long documentId;
    /** 所属资料的处理轮次。 */
    private Long processingVersion;
    /** 本轮分片顺序，从0开始。 */
    private Integer chunkIndex;
    /** 章节标题，无章节时使用资料名。 */
    private String title;
    /** 权威正文，全文检索词项由数据库自动生成。 */
    private String content;
    /** 使用cl100k_base分词器计算的实际Token数量。 */
    private Integer tokenCount;
    /** 是否允许租户内公开检索的权限副本。 */
    private Boolean isPublic;
    /** 向量记录ID，与正文分片ID分别分配。 */
    private Long vectorId;
    /** 分片向量处理状态。 */
    private String embeddingStatus;
    /** 生成向量的完成时间。 */
    private Instant embeddedTime;
}
