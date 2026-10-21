package org.etd.common.permission.constant;

/**
 * 事件中心管理页面对应的菜单资源权限码。
 */
public final class EventPermissionCode {

    /** 事件类型管理菜单资源码，读写操作由框架权限注解判定。 */
    public static final String TYPE = "event:type";
    /** 事件订阅管理菜单资源码，读写操作由框架权限注解判定。 */
    public static final String SUBSCRIPTION = "event:subscription";
    /** 事件消息查询菜单资源码，读写操作由框架权限注解判定。 */
    public static final String MESSAGE = "event:message";
    /** 事件投递查询菜单资源码，读写操作由框架权限注解判定。 */
    public static final String DELIVERY = "event:delivery";

    private EventPermissionCode() {
    }
}
