package org.etd.upms.dict.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;

/**
 * 字典数据项实体类。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "sys_dict_data", excludeProperty = "tenantId")
public class SystemDictDataEntity extends BaseEntity {

    @TableField("dict_type_id")
    private Long dictTypeId;

    @TableField("dict_code")
    private String dictCode;

    @TableField("dict_label")
    private String dictLabel;

    @TableField("dict_value")
    private String dictValue;

    @TableField("sort")
    private Integer sort;

    @TableField(value = "built_in", fill = FieldFill.INSERT)
    @TableFieldExtend("false")
    private Boolean builtIn;

    @TableField(value = "enabled", fill = FieldFill.INSERT)
    @TableFieldExtend("true")
    private Boolean enabled;

    @TableField("remark")
    private String remark;
}
