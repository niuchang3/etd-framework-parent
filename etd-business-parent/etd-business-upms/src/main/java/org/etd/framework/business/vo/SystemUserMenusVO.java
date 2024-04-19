package org.etd.framework.business.vo;

import lombok.Data;

import java.util.Date;

@Data
public class SystemUserMenusVO {

    /**
     * 租户ID
     */
    private Long id;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 菜单名称
     */
    private String menuName;

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
}
