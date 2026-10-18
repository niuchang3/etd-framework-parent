package org.etd.framework.starter.mybaits.permission.model;

import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import java.util.Set;

/** 框架统一的角色数据范围快照，供SQL过滤、创建策略及页面操作能力判断复用。 */
public record DataPermissionScope(
        /** 可信登录用户标识；匿名时为空。 */ Long userId,
        /** 管理员或角色全部数据范围。 */ boolean all,
        /** 是否具有本人数据范围。 */ boolean self,
        /** 组织类型角色最终允许的组织集合。 */ Set<Long> orgIds) {
    /** @return 当前登录用户的数据范围，匿名用户没有任何范围 */
    public static DataPermissionScope current() {
        return from(RequestContext.getUser());
    }

    /**
     * 解析角色权限类型，不接收前端声明的范围。
     * @param user 可信登录用户，可为空
     * @return 不可变数据范围快照
     */
    public static DataPermissionScope from(UserDetails user) {
        if (user == null) {
            return new DataPermissionScope(null, false, false, Set.of());
        }
        Set<String> types = user.getPermissionTypes() == null ? Set.of() : user.getPermissionTypes();
        boolean organization = types.contains(BasicConstant.PermissionType.ORGANIZATION.getCode())
                || types.contains(BasicConstant.PermissionType.ORGANIZATION_AND_SUBORDINATE.getCode())
                || types.contains(BasicConstant.PermissionType.CUSTOM_ORGANIZATION.getCode());
        Set<Long> ids = organization && user.getScopeOrgIds() != null ? Set.copyOf(user.getScopeOrgIds()) : Set.of();
        return new DataPermissionScope(user.getId(), user.isAdmin() || types.contains(BasicConstant.PermissionType.ALL.getCode()),
                types.contains(BasicConstant.PermissionType.SELF.getCode()), ids);
    }

    /**
     * 判断记录是否位于管理范围；公开读取不会扩大管理范围。
     * @param orgId 记录所属组织
     * @param creator 记录创建人
     * @return 是否位于全部、本人或组织范围
     */
    public boolean canManage(Long orgId, Long creator) {
        return all || (self && userId != null && userId.equals(creator)) || (orgId != null && orgIds.contains(orgId));
    }
}
