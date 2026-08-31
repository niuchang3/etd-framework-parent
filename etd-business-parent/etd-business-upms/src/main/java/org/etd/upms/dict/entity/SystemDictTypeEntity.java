package org.etd.upms.dict.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_dict_type")
public class SystemDictTypeEntity extends BaseEntity {

    @TableField("type_code")
    private String typeCode;

    @TableField("type_name")
    private String typeName;

    @TableField("built_in")
    private Boolean builtIn;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("remark")
    private String remark;
}
