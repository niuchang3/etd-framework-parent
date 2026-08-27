package com.etd.framework.starter.client.core.user;

import com.etd.framework.starter.client.core.TenantAuthority;

import java.util.List;

/**
 * 用户权限加载服务。
 * <p>
 * 业务系统实现该接口后，登录用户的角色和权限会写入认证上下文。
 */
public interface PermissionsService {

    /**
     * 根据用户标识加载权限。
     *
     * @param userId 用户标识
     * @return 用户权限集合
     */
    List<TenantAuthority> loadPermissionsByUser(Long userId);


}
