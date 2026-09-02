package org.etd.upms.dict.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;

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

    @TableField("built_in")
    private Boolean builtIn;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("remark")
    private String remark;
}
