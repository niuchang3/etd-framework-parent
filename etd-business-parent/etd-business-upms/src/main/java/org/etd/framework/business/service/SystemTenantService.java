package org.etd.framework.business.service;

import org.etd.framework.business.entity.SystemTenantEntity;

import java.util.List;

public interface SystemTenantService {

    /**
     * 查询用户所在租户信息
     *
     * @param userId
     * @return
     */
    List<SystemTenantEntity> selectByUser(Long userId);
}
