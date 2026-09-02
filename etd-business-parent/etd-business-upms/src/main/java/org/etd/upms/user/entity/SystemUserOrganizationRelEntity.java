package org.etd.upms.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;

/**
 * 用户与组织关联关系实体类。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_user_org_rel")
public class SystemUserOrganizationRelEntity extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("org_id")
    private Long organizationId;

    @TableField("primary_org")
    private Boolean primaryOrganization;
}
