package org.etd.upms.user.controller.vo;

import lombok.Data;

import java.time.Instant;

@Data
public class SystemUserMenusVO {

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 租户ID
     */
    private Long id;
    /**
     * 创建时间
     */
    private Instant createTime;
    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 菜单PATH
     */
    private String menuPath;
    /**
     * 菜单路由
     */
    private String menuRouter;

    /**
     * 菜单Icon
     */
    private String menuIcon;

    /**
     * 排序字段
     */
    private Integer sort;

    /**
     * 角色对菜单的访问级别；租户管理员菜单不限制具体级别。
     */
    private Integer accessLevel;
}
