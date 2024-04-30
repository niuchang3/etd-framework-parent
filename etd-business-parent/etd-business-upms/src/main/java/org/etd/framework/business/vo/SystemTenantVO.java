package org.etd.framework.business.vo;


import lombok.Data;

import java.util.Date;

@Data
public class SystemTenantVO {

    /**
     * 租户ID
     */
    private Long id;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 数据状态
     */
    private Boolean dataStatus;
    /**
     * logo地址
     */
    private String logo;

    /**
     * 父级租户ID
     */

    private Long parentId;
    /**
     * 租户名称
     */
    private String tenantName;
    /**
     * 描述
     */
    private String description;
    /**
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 企业类型
     */
    private String tenantType;
    /**
     * 企业超级管理员
     */
    private Long tenantAdminUser;

    /**
     * 企业超级管理员
     */
    private String adminUser;
    /**
     * 租户锁定
     */
    private Boolean locked;
    /**
     * 租户下的菜单
     */
    private String menus;
}
