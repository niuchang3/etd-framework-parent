package org.etd.upms.role.controller.vo;

import lombok.Data;

import java.time.Instant;

@Data
public class SystemRoleVO {

    private Long id;
    private Instant createTime;
    private Instant updateTime;
    private Integer dataStatus;
    private Boolean builtIn;
    private String roleName;
    private String roleCode;
    private String roleDesc;
    private String permissionType;
}
