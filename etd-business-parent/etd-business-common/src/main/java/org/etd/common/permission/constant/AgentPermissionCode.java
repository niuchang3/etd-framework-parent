package org.etd.common.permission.constant;

/** Agent页面资源码，读写由HTTP动作或接口显式操作声明区分。 */
public final class AgentPermissionCode {
    /** Agent列表、编辑和发布菜单资源，数据维护资格仍由角色范围校验。 */
    public static final String AGENT = "ai:agent";

    private AgentPermissionCode() {
    }
}
