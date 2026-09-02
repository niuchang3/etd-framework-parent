package org.etd.upms.menu.controller.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统菜单保存与更新数据传输对象 DTO。
 */
@Data
public class SystemMenuSaveDTO {

    private Long parentId;

    @Size(max = 10, message = "菜单名称不能超过10个字符")
    private String menuName;

    @Size(max = 100, message = "菜单路径不能超过100个字符")
    private String menuPath;

    @Size(max = 100, message = "菜单路由不能超过100个字符")
    private String menuRouter;

    @Size(max = 200, message = "菜单图标不能超过200个字符")
    private String menuIcon;

    @Size(max = 20, message = "菜单类型不能超过20个字符")
    private String menuType;

    private Integer sort;
}
