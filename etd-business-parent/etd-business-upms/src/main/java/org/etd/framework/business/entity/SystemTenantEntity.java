package org.etd.framework.business.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.etd.framework.starter.mybaits.core.BaseEntity;

import java.io.Serializable;

@Data
@TableName("system_tenant")
public class SystemTenantEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 900389846923149037L;

    /**
     * 父级租户ID
     */
    @TableField("PARENT_ID")
    private Long parentId;
    /**
     * 租户名称
     */
    @TableField("TENANT_NAME")
    private String tenantName;
    /**
     * 统一社会信用代码
     */
    @TableField("CREDIT_CODE")
    private String creditCode;

    /**
     * 企业类型
     */
    @TableField("TENANT_TYPE")
    private String tenantType;
    /**
     * 企业超级管理员
     */
    @TableField("TENANT_ADMIN_USER")
    private Boolean tenantAdminUser;

    /**
     * 租户锁定
     */
    @TableField("LOCKED")
    private Boolean locked;


}
