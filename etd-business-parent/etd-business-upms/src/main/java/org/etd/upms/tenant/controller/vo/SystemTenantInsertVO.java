package org.etd.upms.tenant.controller.vo;

import lombok.Data;

@Data
public class SystemTenantInsertVO {
    /**
     * logo地址
     */
    private String logo;
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
     * 管路员ID
     */
    private Long adminUser;




}
