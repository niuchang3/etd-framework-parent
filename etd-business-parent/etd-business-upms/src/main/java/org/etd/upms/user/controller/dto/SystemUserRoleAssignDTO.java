package org.etd.upms.user.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 用户分配角色关联关系 DTO。
 */
@Data
public class SystemUserRoleAssignDTO {

    @NotNull(message = "角色ID集合不能为空")
    private Set<Long> roleIds = new LinkedHashSet<>();
}
