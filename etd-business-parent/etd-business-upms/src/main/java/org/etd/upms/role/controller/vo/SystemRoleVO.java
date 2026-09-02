package org.etd.upms.role.controller.vo;

import lombok.Data;

import java.time.Instant;

/**
 * 角色视图响应对象 VO。
 */
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
