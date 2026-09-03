package org.etd.framework.common.core.constants;

/** 接口操作类型；AUTO 仅用于注解声明，实际授权只使用 READ、WRITE。 */
public enum PermissionAction {
    AUTO("auto"), READ("read"), WRITE("write");

    private final String code;

    PermissionAction(String code) { this.code = code; }

    public String getCode() { return code; }
}
