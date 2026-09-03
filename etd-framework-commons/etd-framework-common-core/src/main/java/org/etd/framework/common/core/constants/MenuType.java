package org.etd.framework.common.core.constants;

/** 菜单树节点类型，目录仅组织导航结构，不产生接口权限。 */
public enum MenuType {
    DIRECTORY("DIRECTORY"), MENU("MENU");
    private final String code;
    MenuType(String code) { this.code = code; }
    public String getCode() { return code; }
}
