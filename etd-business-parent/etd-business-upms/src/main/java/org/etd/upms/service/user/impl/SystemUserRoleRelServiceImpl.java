package org.etd.upms.service.user.impl;

import org.etd.framework.common.core.user.TenantAuthority;
import com.google.common.collect.Lists;
import org.etd.upms.converter.SystemUserRoleConverter;
import org.etd.upms.mapper.user.SystemUserRoleRelMapper;
import org.etd.upms.service.user.SystemUserRoleRelService;
import org.etd.upms.controller.user.vo.SystemUserRoleVO;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class SystemUserRoleRelServiceImpl implements SystemUserRoleRelService {

    @Autowired
    private SystemUserRoleRelMapper userRoleRelMapper;

    @Override
    public List<SystemUserRoleVO> selectByUser(Long userId) {
        return userRoleRelMapper.selectByUserId(userId);
    }


    /**
     * 根据用户权限加载接口权限
     *
     * @param userId
     * @return
     */
    @IgnoreTenant
    @Override
    public List<TenantAuthority> loadPermissionsByUser(Long userId) {
        List<SystemUserRoleVO> roleVos = selectByUser(userId);
        if (CollectionUtils.isEmpty(roleVos)) {
            return Lists.newArrayList();
        }
        return Mappers.getMapper(SystemUserRoleConverter.class).toTenantAuthority(roleVos);
    }
}
