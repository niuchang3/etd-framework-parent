package org.etd.framework.common.core.constants;

/** 菜单资源码和接口操作权限共同遵循的编码规则。 */
public final class PermissionCode {
    public static final int MAX_LENGTH = 100;
    public static final String RESOURCE_PATTERN = "[a-z][a-z0-9-]*(?::[a-z][a-z0-9-]*)*";
    public static final String SEPARATOR = ":";

    private PermissionCode() { }

    /** 校验人工配置的完整资源码，不追加应用名，不自动更改大小写。 */
    public static boolean isValidResource(String code) {
        return code != null && code.length() <= MAX_LENGTH && code.matches(RESOURCE_PATTERN);
    }

    /** 将菜单资源码展开为具体操作权限，供授权加载和请求校验共用。 */
    public static String createAuthority(String resource, PermissionAction action) {
        if (!isValidResource(resource) || action == null || action == PermissionAction.AUTO) {
            throw new IllegalArgumentException("资源权限码或读写操作无效：" + resource);
        }
        return resource + SEPARATOR + action.getCode();
    }
}
