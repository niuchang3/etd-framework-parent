package org.etd.framework.starter.mybaits.permission;

import com.google.common.collect.Lists;

import java.util.List;

public class DefaultDataPermissionApi implements DataPermissionApi<Long, Long> {


    @Override
    public List<Long> getDeptIds() {
        return Lists.newArrayList(-1L);
    }

    @Override
    public Long getUserId() {
        return -1L;
    }
}
