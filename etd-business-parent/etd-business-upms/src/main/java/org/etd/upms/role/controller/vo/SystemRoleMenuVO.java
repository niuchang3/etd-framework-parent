package org.etd.upms.role.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色菜单关系视图对象 VO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemRoleMenuVO {

    private Long menuId;
    private String accessLevel;
}
