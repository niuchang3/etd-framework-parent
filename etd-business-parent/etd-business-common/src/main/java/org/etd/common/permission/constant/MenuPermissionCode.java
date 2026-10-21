package org.etd.common.permission.constant;

/** UPMS 内置功能菜单对应的资源权限码，与初始化菜单配置保持一致。 */
public final class MenuPermissionCode {
    /** 系统用户管理菜单资源码，读写操作由框架权限注解判定。 */
    public static final String USER = "system:user";
    /** 系统角色管理菜单资源码，读写操作由框架权限注解判定。 */
    public static final String ROLE = "system:role";
    /** 系统菜单管理菜单资源码，读写操作由框架权限注解判定。 */
    public static final String MENU = "system:menu";
    /** 租户管理菜单资源码，读写操作由框架权限注解判定。 */
    public static final String TENANT = "system:tenant";
    /** 组织机构管理菜单资源码，读写操作由框架权限注解判定。 */
    public static final String ORGANIZATION = "system:organization";
    /** 数据字典管理菜单资源码，读写操作由框架权限注解判定。 */
    public static final String DICT = "system:dict";
    /** 系统配置管理菜单资源码，读写操作由框架权限注解判定。 */
    public static final String CONFIG = "system:config";
    private MenuPermissionCode() { }
}
