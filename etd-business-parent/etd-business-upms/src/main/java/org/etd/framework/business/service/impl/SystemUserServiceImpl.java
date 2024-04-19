package org.etd.framework.business.service.impl;

import com.etd.framework.starter.client.core.TenantAuthority;
import com.etd.framework.starter.client.core.user.PermissionsService;
import com.etd.framework.starter.client.core.user.UserDetails;
import org.etd.framework.business.converter.SystemUserConverter;
import org.etd.framework.business.entity.SystemUserEntity;
import org.etd.framework.business.mapper.SystemUserMapper;
import org.etd.framework.business.service.SystemUserService;
import org.etd.framework.starter.mybaits.core.EtdLambdaQueryWrapper;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;

/**
 * 系统用户Service
 */
@Service
public class SystemUserServiceImpl implements SystemUserService {
    /**
     * 系统用户Mapper
     */
    @Autowired
    private SystemUserMapper systemUserMapper;
    /**
     * 用户与角色的关系Service
     */
    @Autowired
    private PermissionsService permissionsService;


    @Override
    public boolean register(UserDetails userDetails) {
        return false;
    }


    @Override
    public UserDetails loadUserById(Long id) {
        SystemUserEntity systemUserEntity = selectByUserById(id);
        if (ObjectUtils.isEmpty(systemUserEntity)) {
            return null;
        }
        return toUserDetails(systemUserEntity);
    }


    @Override
    public UserDetails loadUserByAccount(String account) {
        SystemUserEntity systemUserEntity = selectByAccount(account);
        if (ObjectUtils.isEmpty(systemUserEntity)) {
            return null;
        }
        return toUserDetails(systemUserEntity);
    }

    /**
     * 数据转换
     *
     * @param systemUserEntity
     * @return
     */
    private UserDetails toUserDetails(SystemUserEntity systemUserEntity) {
        List<TenantAuthority> authorities = permissionsService.loadPermissionsByUser(systemUserEntity.getId());
        return toUserDetails(systemUserEntity, authorities);
    }

    /**
     * 数据转换
     *
     * @param systemUserEntity
     * @param authorities
     * @return
     */
    private UserDetails toUserDetails(SystemUserEntity systemUserEntity, List<TenantAuthority> authorities) {
        UserDetails userDetails = Mappers.getMapper(SystemUserConverter.class).toUserDetails(systemUserEntity);
        userDetails.setAuthorities(authorities);
        return userDetails;
    }

    /**
     * 根据用户ID查询用户信息
     *
     * @param id
     * @return
     */
    @Override
    public SystemUserEntity selectByUserById(Long id) {
        return systemUserMapper.selectById(id);
    }

    /**
     * 根据账号查询用户信息
     *
     * @param account
     * @return
     */
    @Override
    public SystemUserEntity selectByAccount(String account) {
        EtdLambdaQueryWrapper<SystemUserEntity> wrapper = new EtdLambdaQueryWrapper<>();
        wrapper.eq(SystemUserEntity::getAccount, account);
        return systemUserMapper.selectOne(wrapper);
    }
}
