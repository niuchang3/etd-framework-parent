package org.etd.upms.user.service.impl;

import com.etd.framework.starter.client.core.user.PermissionsService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.framework.common.core.constants.BasicConstant;
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
import org.springframework.util.StringUtils;

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

    @Override
    public IPage<SystemUserEntity> page(long current, long size, String keyword, Boolean enabled, Boolean locked,
                                        Set<Long> userIds) {
        if (userIds != null && userIds.isEmpty()) {
            return new Page<>(current, size, 0);
        }
        EtdLambdaQueryWrapper<SystemUserEntity> wrapper = new EtdLambdaQueryWrapper<>();
        wrapper.in(userIds != null, SystemUserEntity::getId, userIds)
                .eq(enabled != null, SystemUserEntity::getEnabled, enabled)
                .eq(locked != null, SystemUserEntity::getLocked, locked)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(SystemUserEntity::getAccount, keyword)
                        .or().like(SystemUserEntity::getUserName, keyword)
                        .or().like(SystemUserEntity::getMobile, keyword)
                        .or().like(SystemUserEntity::getNickName, keyword))
                .orderByDesc(SystemUserEntity::getCreateTime, SystemUserEntity::getId);
        return systemUserMapper.selectPage(new Page<>(current, size), wrapper);
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
        userDetails.setOrgId(permissions.getPrimaryOrganizationId());
        userDetails.setOrgIds(permissions.getOrganizationIds());
        userDetails.setPermissionTypes(permissions.getPermissionTypes());
        userDetails.setPermissionType(resolveLegacyPermissionType(permissions.getPermissionTypes()));
        userDetails.setCustomOrgIds(permissions.getCustomOrganizationIds());
        userDetails.setScopeOrgIds(permissions.getScopeOrganizationIds());
        return userDetails;
    }

    private String resolveLegacyPermissionType(Set<String> permissionTypes) {
        if (permissionTypes.contains(BasicConstant.PermissionType.ALL.getCode())) {
            return BasicConstant.PermissionType.ALL.getCode();
        }
        return permissionTypes.size() == 1 ? permissionTypes.iterator().next() : null;
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
    public SystemUserEntity requireExists(Long id) {
        SystemUserEntity entity = selectByUserById(id);
        if (entity == null) {
            throw new ApiRuntimeException("用户不存在。");
        }
        return entity;
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

    @Override
    public Long insert(SystemUserEntity entity, String rawPassword) {
        ensureAccountAvailable(entity.getAccount(), null);
        String mobile = normalizeMobile(entity.getMobile());
        ensureMobileAvailable(mobile, null);
        entity.setAccount(entity.getAccount().trim());
        entity.setMobile(mobile);
        entity.setUserName(entity.getUserName().trim());
        entity.setPassword(passwordEncoder.encode(rawPassword));
        entity.setLocked(false);
        entity.setEnabled(true);
        entity.setDataStatus(BasicConstant.DataStatus.ENABLED.getCode());
        if (systemUserMapper.insert(entity) <= 0) {
            throw new ApiRuntimeException("用户创建失败。");
        }
        return entity.getId();
    }

    @Override
    public boolean update(Long id, SystemUserEntity entity) {
        requireExists(id);
        ensureAccountAvailable(entity.getAccount(), id);
        String mobile = normalizeMobile(entity.getMobile());
        ensureMobileAvailable(mobile, id);
        entity.setId(id);
        entity.setAccount(entity.getAccount().trim());
        entity.setMobile(mobile);
        entity.setUserName(entity.getUserName().trim());
        return systemUserMapper.updateById(entity) > 0;
    }

    @Override
    public boolean delete(Long id) {
        requireExists(id);
        return systemUserMapper.deleteById(id) > 0;
    }

    @Override
    public boolean switchEnabled(Long id, Boolean enabled) {
        requireExists(id);
        SystemUserEntity entity = new SystemUserEntity();
        entity.setId(id);
        entity.setEnabled(enabled);
        return systemUserMapper.updateById(entity) > 0;
    }

    @Override
    public boolean switchLocked(Long id, Boolean locked) {
        requireExists(id);
        SystemUserEntity entity = new SystemUserEntity();
        entity.setId(id);
        entity.setLocked(locked);
        return systemUserMapper.updateById(entity) > 0;
    }

    @IgnoreTenant
    @Override
    public Long createTenantAdmin(Long tenantId, String account, String password, String userName, String mobile) {
        ensureAccountAvailable(account, null);
        String normalizedMobile = normalizeMobile(mobile);
        ensureMobileAvailable(normalizedMobile, null);
        SystemUserEntity entity = new SystemUserEntity();
        entity.setTenantId(tenantId);
        entity.setAccount(account.trim());
        entity.setPassword(passwordEncoder.encode(password));
        entity.setUserName(userName.trim());
        entity.setMobile(normalizedMobile);
        entity.setLocked(false);
        entity.setEnabled(true);
        if (systemUserMapper.insert(entity) <= 0) {
            throw new ApiRuntimeException("租户管理员创建失败。");
        }
        return entity.getId();
    }

    private void ensureAccountAvailable(String account, Long excludedId) {
        if (systemUserMapper.selectAccountCount(account.trim(), excludedId) > 0) {
            throw new ApiRuntimeException("登录账号已存在。");
        }
    }

    private void ensureMobileAvailable(String mobile, Long excludedId) {
        if (mobile != null && systemUserMapper.selectMobileCount(mobile, excludedId) > 0) {
            throw new ApiRuntimeException("手机号码已存在。");
        }
    }

    private String normalizeMobile(String mobile) {
        return StringUtils.hasText(mobile) ? mobile.trim() : null;
    }
}
