package org.etd.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.etd.ai.knowledge.model.KnowledgeChunkSource;
import org.etd.ai.knowledge.mapper.typehandler.KnowledgeChunkSourceTypeHandler;
import org.etd.framework.starter.mybaits.core.OrgScopedEntity;

/** 可编辑的文档分片正文，不保存向量和检索索引。 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true)
@TableName(value = "ai_knowledge_chunk", autoResultMap = true)
public class KnowledgeChunkEntity extends OrgScopedEntity {
    /** 继承所属资料的共享标记，默认false；仅扩大读取范围，不单独设置。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("false")
    private Boolean isPublic;

    /** 所属资料ID，全部操作先通过资料检查权限。 */
    private Long documentId;
    /** 当前资料内的顺序，从0开始；合并后允许存在空号。 */
    private Integer chunkIndex;
    /** 权威正文，用户编辑和合并均更新此字段。 */
    private String content;
    /** 以cl100k_base计算的正文Token数，与Docling原生分片预算可使用不同分词器。 */
    private Integer tokenCount;
    /** 来源标题、页码及结构块引用。 */
    @TableField(typeHandler = KnowledgeChunkSourceTypeHandler.class)
    private KnowledgeChunkSource source;
}
