package org.etd.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.etd.framework.starter.mybaits.core.OrgScopedEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;

/** KnowledgeFolderEntity 持久化记录；租户及审计字段由框架可信上下文填充。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_knowledge_folder")
public class KnowledgeFolderEntity extends OrgScopedEntity {
    /** 非负乐观锁版本，修改时必须提交当前值。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    @Version
    private Long version;

    /** 所属知识库标识。 */
    private Long knowledgeBaseId;

    /** 父目录，根目录为空。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long parentId;

    /** 同级唯一目录名称。 */
    private String folderName;

    /** 非负同级排序。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    private Integer sortOrder;

    /** 租户内公开读取，默认关闭，不传递给子项。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("false")
    private Boolean isPublic;
}
