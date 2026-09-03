package org.etd.upms.menu.entity;

/** 从有效菜单和角色关系查询得到的资源授权，供权限加载使用。 */
public record MenuPermissionGrant(String permissionCode, String accessLevel) { }
