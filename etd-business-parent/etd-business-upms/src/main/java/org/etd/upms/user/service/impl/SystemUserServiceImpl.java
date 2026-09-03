package org.etd.upms.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.common.core.user.UserPermissions;
import org.etd.upms.user.converter.SystemUserConverter;
import org.etd.upms.user.entity.SystemUserEntity;
import org.etd.upms.user.mapper.SystemUserMapper;
import org.etd.upms.user.service.SystemUserService;
import org.etd.upms.tenant.service.SystemTenantService;
import org.etd.framework.starter.mybaits.core.EtdLambdaQueryWrapper;
import org.etd.framework.starter.mybaits.permission.annotation.IgnoreDataPermission;
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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SystemTenantService tenantService;

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

    @Override
    public IPage<SystemUserEntity> selectUserPage(long current, long size, String keyword, Set<Long> orgIds,
                                                   Boolean enabled, Boolean locked) {
        return systemUserMapper.selectUserPage(new Page<>(current, size), keyword, orgIds, enabled, locked);
    }

    /**
     * 根据用户ID查询用户信息
     *
     * @param id
     * @return
     */
    /**
     * 查询 By User By Id
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @Override
    public SystemUserEntity selectByUserById(Long id) {
        return systemUserMapper.selectById(id);
    }

    /**
     * 校验并要求 Exists
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @Override
    public SystemUserEntity requireExists(Long id) {
        SystemUserEntity entity = selectByUserById(id);
        if (entity == null) {
            throw new ApiRuntimeException("用户不存在。");
        }
        return entity;
    }

    /**
     * 查询 By User By Id
     *
     * @param ids 参数 ids
     * @return 处理结果
     */
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

    /**
     * 查询 User Ids By Tenant Id
     *
     * @param tenantId 参数 tenantId
     * @return 处理结果
     */
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
    /**
     * 查询 By Account
     *
     * @param account 参数 account
     * @return 处理结果
     */
    @Override
    public SystemUserEntity selectByAccount(String account) {
        EtdLambdaQueryWrapper<SystemUserEntity> wrapper = new EtdLambdaQueryWrapper<>();
        wrapper.eq(SystemUserEntity::getAccount, account);
        return systemUserMapper.selectOne(wrapper);
    }

    /**
     * 新增保存
     *
     * @param entity 参数 entity
     * @param rawPassword 参数 rawPassword
     * @return 处理结果
     */
    @Override
    public Long insert(SystemUserEntity entity, String rawPassword) {
        ensureAccountAvailable(entity.getAccount(), null);
        String mobile = normalizeMobile(entity.getMobile());
        ensureMobileAvailable(mobile, null);
        entity.setAccount(entity.getAccount().trim());
        entity.setMobile(mobile);
        entity.setUserName(entity.getUserName().trim());
        entity.setPassword(passwordEncoder.encode(rawPassword));
        if (systemUserMapper.insert(entity) <= 0) {
            throw new ApiRuntimeException("用户创建失败。");
        }
        return entity.getId();
    }

    /**
     * 更新修改
     *
     * @param id 参数 id
     * @param entity 参数 entity
     * @return 处理结果
     */
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

    /**
     * 删除
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @Override
    public boolean delete(Long id) {
        requireExists(id);
        return systemUserMapper.deleteById(id) > 0;
    }

    /**
     * 切换 Enabled
     *
     * @param id 参数 id
     * @param enabled 参数 enabled
     * @return 处理结果
     */
    @Override
    public boolean switchEnabled(Long id, Boolean enabled) {
        requireExists(id);
        SystemUserEntity entity = new SystemUserEntity();
        entity.setId(id);
        entity.setEnabled(enabled);
        return systemUserMapper.updateById(entity) > 0;
    }

    /**
     * 切换 Locked
     *
     * @param id 参数 id
     * @param locked 参数 locked
     * @return 处理结果
     */
    @Override
    public boolean switchLocked(Long id, Boolean locked) {
        requireExists(id);
        SystemUserEntity entity = new SystemUserEntity();
        entity.setId(id);
        entity.setLocked(locked);
        return systemUserMapper.updateById(entity) > 0;
    }

    /**
     * 创建 Tenant Admin
     *
     * @param tenantId 参数 tenantId
     * @param account 参数 account
     * @param password 参数 password
     * @param userName 参数 userName
     * @param mobile 参数 mobile
     * @return 处理结果
     */
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
        if (systemUserMapper.insert(entity) <= 0) {
            throw new ApiRuntimeException("租户管理员创建失败。");
        }
        return entity.getId();
    }

    @Override
    public void updatePrimaryOrganization(Long userId, Long primaryOrgId) {
        if (userId == null) {
            return;
        }
        SystemUserEntity entity = new SystemUserEntity();
        entity.setId(userId);
        entity.setOrgId(primaryOrgId);
        systemUserMapper.updateById(entity);
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
