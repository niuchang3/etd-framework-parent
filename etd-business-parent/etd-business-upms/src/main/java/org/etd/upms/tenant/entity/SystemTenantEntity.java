package org.etd.upms.tenant.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "sys_tenant", excludeProperty = "tenantId")
public class SystemTenantEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 900389846923149037L;

    @TableField("logo")
    private String logo;
    /**
     * 租户名称
     */
    @TableField("tenant_name")
    private String tenantName;
    /**
     * 描述
     */
    @TableField("description")
    private String description;
    /**
     * 统一社会信用代码
     */
    @TableField("credit_code")
    private String creditCode;

    /**
     * 企业类型
     */
    @TableField("tenant_type")
    private String tenantType;
    /**
     * 企业超级管理员
     */
    @TableField("tenant_admin_user")
    private Long tenantAdminUser;

    /**
     * 租户锁定
     */
    @TableField("locked")
    private Boolean locked;


    @TableField("menus")
    private String menus;


}
