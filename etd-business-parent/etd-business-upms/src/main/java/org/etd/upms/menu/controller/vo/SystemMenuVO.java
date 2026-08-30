package org.etd.upms.menu.controller.vo;

import lombok.Data;

import java.util.Date;

@Data
public class SystemMenuVO {

    private Long id;

    private Long parentId;

    private Date createTime;

    private Integer dataStatus;

    private String menuName;

    private String menuPath;

    private String menuRouter;

    private String menuIcon;

    private String menuType;

    private Integer sort;
}
