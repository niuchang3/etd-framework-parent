package org.etd.framework.starter.mybaits.permission;

import java.io.Serializable;
import java.util.List;

public interface DataPermissionApi<DeptPK extends Serializable, UserPk extends Serializable> {


    /**
     * 获取部门Ids
     * size 为 1  并且值 为 -1,则不添加权限
     *
     * @return
     */
    List<DeptPK> getDeptIds();

    /**
     * 获取用户ID
     * -1的情况下，不添加权限
     *
     * @return
     */
    UserPk getUserId();
}
