package org.etd.upms.user.service.impl;

import com.etd.framework.starter.client.core.user.PermissionsService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.RoleAuthority;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.common.core.user.UserPermissions;
import org.etd.upms.user.converter.SystemUserConverter;
import org.etd.upms.user.entity.SystemUserEntity;
import org.etd.upms.user.mapper.SystemUserMapper;
import org.etd.upms.user.service.SystemUserService;
import org.etd.upms.tenant.service.SystemTenantService;
import org.etd.framework.starter.mybaits.core.EtdLambdaQueryWrapper;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.LinkedHashSet;
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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SystemTenantService tenantService;

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
        UserDetails userDetails = toUserDetails(systemUserEntity, permissions);
        disableUserWhenTenantUnavailable(userDetails);
        return userDetails;
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

    private void disableUserWhenTenantUnavailable(UserDetails userDetails) {
        // 平台管理员承担租户维护职责，即使所属租户停用也保留登录能力。
        if (!userDetails.isPlatformAdmin() && !tenantService.isLoginEnabled(userDetails.getTenantId())) {
            // 不在用户加载阶段抛业务异常，交由 Security 统一执行用户禁用校验。
            userDetails.setEnabled(false);
        }
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
    @IgnoreTenant
    public List<SystemUserEntity> selectByUserById(Set<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        EtdLambdaQueryWrapper<SystemUserEntity> wrapper = new EtdLambdaQueryWrapper<>();
        wrapper.in(SystemUserEntity::getId, ids);
        return systemUserMapper.selectList(wrapper);
    }

    @Override
    @IgnoreTenant
    public Set<Long> selectUserIdsByTenantId(Long tenantId) {
        EtdLambdaQueryWrapper<SystemUserEntity> wrapper = new EtdLambdaQueryWrapper<>();
        wrapper.eq(SystemUserEntity::getTenantId, tenantId)
                .select(SystemUserEntity::getId);
        Set<Long> userIds = new LinkedHashSet<>();
        systemUserMapper.selectList(wrapper).forEach(user -> userIds.add(user.getId()));
        return userIds;
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

    @IgnoreTenant
    @Override
    public Long createTenantAdmin(Long tenantId, String account, String password, String userName, String mobile) {
        ensureAccountAvailable(account);
        SystemUserEntity entity = new SystemUserEntity();
        entity.setTenantId(tenantId);
        entity.setAccount(account.trim());
        entity.setPassword(passwordEncoder.encode(password));
        entity.setUserName(userName.trim());
        entity.setMobile(mobile);
        entity.setLocked(false);
        entity.setEnabled(true);
        if (systemUserMapper.insert(entity) <= 0) {
            throw new ApiRuntimeException("租户管理员创建失败。");
        }
        return entity.getId();
    }

    private void ensureAccountAvailable(String account) {
        EtdLambdaQueryWrapper<SystemUserEntity> wrapper = new EtdLambdaQueryWrapper<>();
        wrapper.eq(SystemUserEntity::getAccount, account.trim());
        if (systemUserMapper.selectCount(wrapper) > 0) {
            throw new ApiRuntimeException("管理员账号已存在。");
        }
    }
}
