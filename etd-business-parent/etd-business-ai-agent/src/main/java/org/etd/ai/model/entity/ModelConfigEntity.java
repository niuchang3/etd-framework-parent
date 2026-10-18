package org.etd.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.etd.framework.starter.mybaits.core.BaseEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;

/** ModelConfigEntity 持久化记录；租户及审计字段由框架可信上下文填充。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_model_config")
public class ModelConfigEntity extends BaseEntity {
    /** 非负乐观锁版本，修改时必须提交当前值。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    @Version
    private Long version;

    /** 本租户连接标识。 */
    private Long connectionId;

    /** 模型显示名称。 */
    private String modelName;

    /** 模型主能力分类。 */
    private String modelType;

    /** 厂商真实模型代码。 */
    private String modelCode;

    /** 后端实测输出维度，基础阶段为空且页面只读。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer embeddingDimensions;

    /** 用途说明。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
}
