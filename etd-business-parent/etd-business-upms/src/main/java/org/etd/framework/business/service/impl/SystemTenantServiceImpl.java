package org.etd.framework.business.service.impl;

import org.etd.framework.business.converter.SystemTenantConvert;
import org.etd.framework.business.entity.SystemTenantEntity;
import org.etd.framework.business.mapper.SystemTenantMapper;
import org.etd.framework.business.service.SystemTenantService;
import org.etd.framework.business.service.SystemUserRoleRelService;
import org.etd.framework.business.vo.SystemTenantVO;
import org.etd.framework.business.vo.SystemUserRoleVO;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.core.EtdLambdaQueryWrapper;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SystemTenantServiceImpl implements SystemTenantService {

    @Autowired
    private SystemTenantMapper systemTenantMapper;

    @Autowired
    private SystemUserRoleRelService userRoleRelService;

    @Override
    public List<SystemTenantVO> selectByUser(Long userId) {
        List<SystemUserRoleVO> roleVOS = userRoleRelService.selectByUser(userId);
        if (CollectionUtils.isEmpty(roleVOS)) {
            throw new ApiRuntimeException("用户身份信息异常,请联系管理员处理");
        }
        List<Long> tenantIds = roleVOS.stream().map(SystemUserRoleVO::getTenantId).collect(Collectors.toList());
        EtdLambdaQueryWrapper<SystemTenantEntity> wrapper = new EtdLambdaQueryWrapper<>();
        wrapper.in(SystemTenantEntity::getId, tenantIds);
        List<SystemTenantEntity> entities = systemTenantMapper.selectList(wrapper);
        return Mappers.getMapper(SystemTenantConvert.class).toVo(entities);
    }
}
