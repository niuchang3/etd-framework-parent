package org.etd.upms.tenant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.upms.tenant.entity.SystemTenantEntity;
import org.etd.upms.tenant.controller.vo.SystemTenantVO;

import java.util.List;
import java.util.Set;

public interface SystemTenantService {

    List<SystemTenantVO> selectAll();

    List<SystemTenantVO> selectByIds(Set<Long> tenantIds);

    /**
     * 获取当前用户所在租户信息
     *
     * @return
     */
    SystemTenantVO selectCurrentTenant();


    /**
     * 分页查询租户信息
     *
     * @param page
     * @param times
     * @param keyword
     * @return
     */
    IPage<SystemTenantVO> page(IPage<SystemTenantEntity> page, List<String> times, String keyword);

    Long insert(SystemTenantEntity entity);

    boolean bindAdminUser(Long tenantId, Long adminUserId);

    boolean update(Long tenantId, SystemTenantEntity entity);

    boolean switchStatus(Long tenantId, Integer status);

    boolean switchLocked(Long tenantId, boolean locked);

    boolean delete(Long tenantId);

    boolean isLoginEnabled(Long tenantId);
}
