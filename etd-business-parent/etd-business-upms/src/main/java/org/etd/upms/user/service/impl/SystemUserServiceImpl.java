package org.etd.upms.user.service.impl;

import com.etd.framework.starter.client.core.user.PermissionsService;
import org.etd.framework.common.core.user.RoleAuthority;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.common.core.user.UserPermissions;
import org.etd.upms.user.converter.SystemUserConverter;
import org.etd.upms.user.entity.SystemUserEntity;
import org.etd.upms.user.mapper.SystemUserMapper;
import org.etd.upms.user.service.SystemUserService;
import org.etd.framework.starter.mybaits.core.EtdLambdaQueryWrapper;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

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


    @IgnoreTenant
    @Override
    public UserDetails loadUserById(Long id) {
        SystemUserEntity systemUserEntity = selectByUserById(id);
        if (ObjectUtils.isEmpty(systemUserEntity)) {
            return null;
        }
        return toUserDetails(systemUserEntity);
    }

    @IgnoreTenant
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
        UserPermissions permissions = permissionsService.loadPermissionsByUser(systemUserEntity.getId());
        validateTenant(systemUserEntity, permissions);
        return toUserDetails(systemUserEntity, permissions);
    }

    /**
     * 数据转换
     *
     * @param systemUserEntity
     * @param permissions
     * @return
     */
    private UserDetails toUserDetails(SystemUserEntity systemUserEntity, UserPermissions permissions) {
        UserDetails userDetails = Mappers.getMapper(SystemUserConverter.class).toUserDetails(systemUserEntity);
        userDetails.setTenantId(systemUserEntity.getTenantId());
        userDetails.setRoleCodes(permissions.getRoleCodes());
        userDetails.setAuthorities(permissions.getRoleCodes().stream()
                .map(RoleAuthority::new)
                .toList());
        userDetails.setPlatformAdmin(permissions.getPlatformAdmin());
        userDetails.setTenantAdmin(permissions.getTenantAdmin());
        return userDetails;
    }

    /**
     * 用户主表与角色关系必须指向同一个租户，避免跨租户权限被合并到登录态。
     */
    private void validateTenant(SystemUserEntity user, UserPermissions permissions) {
        if (permissions.getTenantId() != null && !Objects.equals(user.getTenantId(), permissions.getTenantId())) {
            throw new IllegalStateException("用户主表与角色关系的租户不一致，用户ID：" + user.getId());
        }
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

    @Override
    public List<SystemUserEntity> selectByUserById(Set<Long> ids) {
        EtdLambdaQueryWrapper<SystemUserEntity> wrapper = new EtdLambdaQueryWrapper<>();
        wrapper.in(SystemUserEntity::getId,ids);
        return systemUserMapper.selectList(wrapper);
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
