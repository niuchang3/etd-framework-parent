package org.etd.framework.business.vo;

import lombok.Data;

import java.util.List;

@Data
public class Tenant {
    /**
     * 租户ID
     */
    private Long id;
    /**
     * 租户名称
     */
    private String tenantName;


    private List<Authority> authorities;
}
