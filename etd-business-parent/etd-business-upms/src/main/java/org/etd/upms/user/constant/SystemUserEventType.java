package org.etd.upms.user.constant;

/**
 * 系统用户领域事件类型，作为发送端与事件中心之间的稳定协议编码。
 */
public final class SystemUserEventType {

    /** 用户及其初始角色、组织关系全部创建成功。 */
    public static final String USER_CREATED = "upms.user.created";

    private SystemUserEventType() {
    }
}
