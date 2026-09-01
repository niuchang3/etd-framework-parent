package org.etd.upms.menu.controller.vo;

import lombok.Data;

import java.time.Instant;

@Data
public class SystemMenuVO {

    private Long id;

    private Long parentId;

    private Instant createTime;

    private Integer dataStatus;

    private String menuName;

    private String menuPath;

    private String menuRouter;

    private String menuIcon;

    private String menuType;

    private Integer sort;
}
