package com.etd.framework.starter.client.core.user.memory;

import org.etd.framework.common.core.user.TenantAuthority;
import com.etd.framework.starter.client.core.user.PermissionsService;

import java.util.Collections;
import java.util.List;

/**
 * 内存权限服务兜底实现。
 * <p>
 * 业务系统没有提供 {@link PermissionsService} Bean 时使用，默认不返回任何权限。
 */
public class MemoryPermissionsServiceImpl implements PermissionsService {

    /**
     * 加载用户权限。
     *
     * @param userId 用户标识
     * @return 兜底实现不提供权限，返回空集合
     */
    @Override
    public List<TenantAuthority> loadPermissionsByUser(Long userId) {
        return Collections.emptyList();
    }
}
