package org.etd.upms.role.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色菜单分配 DTO。
 */
@Data
public class SystemRoleMenuAssignDTO {

    @Valid
    @NotNull(message = "菜单授权列表不能为空")
    private List<SystemRoleMenuGrantDTO> menus;
}
