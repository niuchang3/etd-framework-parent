package org.etd.upms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;


@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_user_role_rel")
public class SystemUserRoleRelEntity extends BaseEntity {
    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private String tenantId;
    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;
    /**
     * 角色ID
     */
    @TableField("role_id")
    private Long roleId;



}
