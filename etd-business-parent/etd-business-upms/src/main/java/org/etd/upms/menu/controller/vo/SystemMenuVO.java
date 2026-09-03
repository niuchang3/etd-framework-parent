package org.etd.upms.menu.controller.vo;

import lombok.Data;

import java.time.Instant;

/**
 * 系统菜单视图响应对象 VO。
 */
@Data
public class SystemMenuVO {

    private Long id;

    private Long parentId;

    private Instant createTime;

    private Integer dataStatus;

    private String menuName;

    private String menuPath;

    private String menuRouter;

    /** 人工配置的完整资源权限码；目录和纯导航节点为空。 */
    private String permissionCode;

    private String menuIcon;

    private String menuType;

    private Integer sort;
}
