package org.etd.upms.config.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;

/**
 * 系统参数配置实体类。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "sys_config", excludeProperty = "tenantId")
public class SystemConfigEntity extends BaseEntity {

    @TableField("parameter_key")
    private String parameterKey;

    @TableField("parameter_name")
    private String parameterName;

    @TableField("parameter_value")
    private String parameterValue;

    @TableField("value_type")
    private String valueType;

    @TableField(value = "built_in", fill = FieldFill.INSERT)
    @TableFieldExtend("false")
    private Boolean builtIn;

    @TableField(value = "enabled", fill = FieldFill.INSERT)
    @TableFieldExtend("true")
    private Boolean enabled;

    @TableField("remark")
    private String remark;
}
