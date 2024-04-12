package org.etd.framework.business.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.etd.framework.starter.mybaits.core.BaseEntity;


@Data
@TableName("system_user_role_rel")
public class SystemUserRoleRelEntity extends BaseEntity {
    /**
     * 租户ID
     */
    @TableField("TENANT_ID")
    private String tenantId;
    /**
     * 用户ID
     */
    @TableField("USER_ID")
    private Long userId;
    /**
     * 角色ID
     */
    @TableField("ROLE_ID")
    private Long roleId;



}
