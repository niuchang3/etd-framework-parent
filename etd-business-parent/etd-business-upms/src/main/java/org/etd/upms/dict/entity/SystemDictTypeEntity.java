package org.etd.upms.dict.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;

/**
 * 字典类型实体类。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "sys_dict_type", excludeProperty = "tenantId")
public class SystemDictTypeEntity extends BaseEntity {

    @TableField("type_code")
    private String typeCode;

    @TableField("type_name")
    private String typeName;

    @TableField(value = "built_in", fill = FieldFill.INSERT)
    @TableFieldExtend("false")
    private Boolean builtIn;

    @TableField(value = "enabled", fill = FieldFill.INSERT)
    @TableFieldExtend("true")
    private Boolean enabled;

    @TableField("remark")
    private String remark;
}
