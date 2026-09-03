package org.etd.upms.menu.controller.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;
import org.etd.framework.common.core.constants.PermissionCode;
import org.etd.framework.common.core.constants.MenuType;
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

    /** 人工配置的完整资源权限码；目录和纯导航节点为空。 */
    @Size(max = PermissionCode.MAX_LENGTH, message = "权限码不能超过100个字符")
    @Pattern(regexp = PermissionCode.RESOURCE_PATTERN, message = "权限码使用小写字母开头的字母、数字、中划线分段，分段以冒号连接")
    private String permissionCode;

    @Size(max = 200, message = "菜单图标不能超过200个字符")
    private String menuIcon;

    @Size(max = 20, message = "菜单类型不能超过20个字符")
    private String menuType;

    private Integer sort;

    /** 目录只组织树结构，不能绑定业务接口权限。 */
    @AssertTrue(message = "目录节点不能配置权限码")
    public boolean isDirectoryPermissionValid() {
        return !MenuType.DIRECTORY.getCode().equals(menuType) || permissionCode == null;
    }
}
