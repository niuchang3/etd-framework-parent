package org.etd.upms.role.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_role_menu_rel")
public class SystemRoleMenuRelEntity extends BaseEntity {

    @TableField("role_id")
    private Long roleId;

    @TableField("menu_id")
    private Long menuId;

    @TableField("access_level")
    private String accessLevel;
}
