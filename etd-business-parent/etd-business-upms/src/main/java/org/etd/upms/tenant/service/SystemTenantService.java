package org.etd.upms.tenant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.upms.tenant.entity.SystemTenantEntity;
import org.etd.upms.tenant.controller.vo.SystemTenantVO;

import java.util.List;
import java.util.Set;
import java.time.Instant;

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
    IPage<SystemTenantVO> page(IPage<SystemTenantEntity> page, List<Instant> times, String keyword);

    Long insert(SystemTenantEntity entity);

    boolean bindAdminUser(Long tenantId, Long adminUserId);

    boolean update(Long tenantId, SystemTenantEntity entity);

    boolean switchStatus(Long tenantId, Integer status);

    boolean switchLocked(Long tenantId, boolean locked);

    boolean delete(Long tenantId);

    /**
     * 按租户标识获取租户实体，用于租户安全状态判定。
     *
     * @param tenantId 租户标识
     * @return 租户实体，不存在时返回 null
     */
    SystemTenantEntity fetchById(Long tenantId);

    void requireOrdinary(Long tenantId);
}
