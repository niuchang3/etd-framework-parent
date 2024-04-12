package org.etd.framework.business.service.impl;

import org.etd.framework.business.entity.SystemTenantEntity;
import org.etd.framework.business.mapper.SystemTenantMapper;
import org.etd.framework.business.service.SystemTenantService;
import org.etd.framework.starter.mybaits.core.EtdLambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemTenantServiceImpl implements SystemTenantService {

    @Autowired
    private SystemTenantMapper systemTenantMapper;

    @Override
    public List<SystemTenantEntity> selectByUser(Long userId) {
        EtdLambdaQueryWrapper<SystemTenantEntity> wrapper = new EtdLambdaQueryWrapper<>();

        return null;
    }
}
