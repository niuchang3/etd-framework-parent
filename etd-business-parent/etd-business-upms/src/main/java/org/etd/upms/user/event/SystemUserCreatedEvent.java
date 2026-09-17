package org.etd.upms.user.event;

import java.util.List;
import java.util.Objects;

/**
 * 系统用户创建事件载荷，不包含密码、手机号等敏感信息。
 *
 * @param userId 新建用户主键，同时作为事件分区键
 * @param tenantId 用户所属租户主键
 * @param account 用户登录账号
 * @param userName 用户显示名称
 * @param primaryOrganizationId 用户主组织主键，未指定时为空
 * @param roleIdList 创建时分配的角色主键列表
 * @param organizationIdList 创建时分配的组织主键列表
 */
public record SystemUserCreatedEvent(
        Long userId,
        Long tenantId,
        String account,
        String userName,
        Long primaryOrganizationId,
        List<Long> roleIdList,
        List<Long> organizationIdList
) {

    public SystemUserCreatedEvent {
        Objects.requireNonNull(userId, "用户主键不能为空");
        Objects.requireNonNull(tenantId, "租户主键不能为空");
        Objects.requireNonNull(account, "用户登录账号不能为空");
        Objects.requireNonNull(userName, "用户显示名称不能为空");
        roleIdList = List.copyOf(Objects.requireNonNull(roleIdList, "角色主键列表不能为空"));
        organizationIdList = List.copyOf(
                Objects.requireNonNull(organizationIdList, "组织主键列表不能为空"));
    }
}
