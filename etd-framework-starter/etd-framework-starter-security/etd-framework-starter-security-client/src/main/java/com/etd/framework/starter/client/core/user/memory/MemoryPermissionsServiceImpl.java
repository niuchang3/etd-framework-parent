package com.etd.framework.starter.client.core.user.memory;

import com.etd.framework.starter.client.core.TenantAuthority;
import com.etd.framework.starter.client.core.user.PermissionsService;
import com.etd.framework.starter.client.core.user.UserDetails;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

public class MemoryPermissionsServiceImpl  implements PermissionsService {


    private Map<String, List<TenantAuthority>> userDetailsMap = Maps.newConcurrentMap();

    @Override
    public List<TenantAuthority> loadPermissionsByUser(Long userId) {
        return null;
    }
}
