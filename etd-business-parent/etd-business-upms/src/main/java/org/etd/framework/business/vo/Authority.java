package org.etd.framework.business.vo;


import lombok.Data;

@Data
public class Authority {

    /**
     * 权限id
     */
    private Long id;
    /**
     * 权限父级ID
     */
    private Long parentId;

    /**
     * 全险码名称
     */
    private String authorityName;
    /**
     * 权限码CODE
     */
    private String authority;


}
