package org.etd.framework.starter.mybaits.permission;

import com.google.common.collect.Lists;
import org.etd.framework.starter.mybaits.permission.constant.DataPermissionConstant;

import java.util.List;

public class DefaultDataPermissionApi implements DataPermissionApi<Long, Long> {


    @Override
    public List<Long> getDeptIds() {
        return Lists.newArrayList(DataPermissionConstant.EMPTY_SCOPE_ID);
    }

    @Override
    public Long getUserId() {
        return DataPermissionConstant.EMPTY_SCOPE_ID;
    }
}
