package org.etd.framework.starter.mybaits.permission;

import java.io.Serializable;
import java.util.List;

public interface DataPermissionApi<DeptPK extends Serializable, UserPk extends Serializable> {


    /**
     * 获取部门Ids
     * 仅包含 {@code DataPermissionConstant.EMPTY_SCOPE_ID} 时不添加权限
     *
     * @return
     */
    List<DeptPK> getDeptIds();

    /**
     * 获取用户ID
     * 返回 {@code DataPermissionConstant.EMPTY_SCOPE_ID} 时不添加权限
     *
     * @return
     */
    UserPk getUserId();
}
