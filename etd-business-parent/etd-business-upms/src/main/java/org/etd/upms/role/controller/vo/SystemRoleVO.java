package org.etd.upms.role.controller.vo;

import lombok.Data;

import java.util.Date;

@Data
public class SystemRoleVO {

    private Long id;
    private Date createTime;
    private Date updateTime;
    private Integer dataStatus;
    private Boolean builtIn;
    private String roleName;
    private String roleCode;
    private String roleDesc;
    private String permissionType;
}
