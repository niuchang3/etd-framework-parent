package com.etd.framework.starter.client.core.user;

import com.etd.framework.starter.client.core.TenantAuthority;

import java.util.List;

public interface PermissionsService {

    List<TenantAuthority> loadPermissionsByUser(Long userId);


}
