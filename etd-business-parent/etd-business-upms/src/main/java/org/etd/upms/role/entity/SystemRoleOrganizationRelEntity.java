package org.etd.upms.role.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;

/**
 * 角色与组织关联关系实体类。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_role_org_rel")
public class SystemRoleOrganizationRelEntity extends BaseEntity {

    @TableField("role_id")
    private Long roleId;

    @TableField("org_id")
    private Long organizationId;
}
