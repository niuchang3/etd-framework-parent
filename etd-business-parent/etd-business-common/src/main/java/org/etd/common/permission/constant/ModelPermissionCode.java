package org.etd.common.permission.constant;

/** 模型域菜单资源码，read/write操作由框架根据HTTP方法判定。 */
public final class ModelPermissionCode {
    /** 首页模型广场资源码，统一授权模型浏览和连接接入管理。 */
    public static final String SQUARE = "ai:model-square";
    /** 平台模型目录维护权限，不作为租户浏览首页的前置权限。 */
    public static final String MODEL = "ai:model";

    private ModelPermissionCode() {}
}
