package org.etd.upms.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.etd.upms.role.controller.dto.SystemRoleSaveDTO;
import org.etd.upms.role.controller.vo.SystemRoleVO;
import org.etd.upms.role.entity.SystemRoleEntity;
import org.etd.upms.role.mapper.SystemRoleMapper;
import org.etd.upms.role.service.SystemRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
public class SystemRoleServiceImpl implements SystemRoleService {

    @Autowired
    private SystemRoleMapper roleMapper;

    @Override
    public IPage<SystemRoleVO> page(long current, long size, String keyword, Integer dataStatus) {
        return page(current, size, keyword, dataStatus, false);
    }

    @Override
    public IPage<SystemRoleVO> page(long current, long size, String keyword, Integer dataStatus, Boolean assignableOnly) {
        LambdaQueryWrapper<SystemRoleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dataStatus != null, SystemRoleEntity::getDataStatus, dataStatus)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(SystemRoleEntity::getRoleName, keyword)
                        .or().like(SystemRoleEntity::getRoleCode, keyword));
        if (Boolean.TRUE.equals(assignableOnly)) {
            wrapper.ne(SystemRoleEntity::getRoleCode, BasicConstant.SystemRole.PLATFORM_ADMIN.getCode())
                    .ne(SystemRoleEntity::getRoleCode, BasicConstant.SystemRole.TENANT_ADMIN.getCode());
        }
        wrapper.orderByDesc(SystemRoleEntity::getBuiltIn)
                .orderByAsc(SystemRoleEntity::getRoleName);
        return roleMapper.selectPage(new Page<>(current, size), wrapper).convert(this::toVO);
    }

    @Override
    public SystemRoleVO selectById(Long id) {
        return toVO(roleMapper.selectById(id));
    }

    @Override
    public void requireExists(Long id) {
        requireRole(id);
    }

    @Override
    public void requireWritable(Long id, String message) {
        SystemRoleEntity role = requireRole(id);
        if (Boolean.TRUE.equals(role.getBuiltIn())) {
            throw new ApiRuntimeException(message);
        }
    }

    @Override
    public void requireEnabled(Set<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SystemRoleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemRoleEntity::getId, ids)
                .eq(SystemRoleEntity::getDataStatus, BasicConstant.DataStatus.ENABLED.getCode());
        if (roleMapper.selectCount(wrapper) != ids.size()) {
            throw new ApiRuntimeException("只能为用户分配当前租户下已启用的角色。");
        }
    }

    @Override
    public void requireAssignable(Set<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SystemRoleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemRoleEntity::getId, ids)
                .eq(SystemRoleEntity::getDataStatus, BasicConstant.DataStatus.ENABLED.getCode());
        List<SystemRoleEntity> roles = roleMapper.selectList(wrapper);
        if (roles.size() != ids.size()) {
            throw new ApiRuntimeException("只能为用户分配当前租户下已启用的角色。");
        }
        if (roles.stream().anyMatch(this::isProtectedAdminRole)) {
            throw new ApiRuntimeException("禁止手工分配平台管理员或租户管理员角色。");
        }
    }

    @Override
    public Long insert(SystemRoleSaveDTO dto) {
        ensureCodeAvailable(dto.getRoleCode(), null);
        SystemRoleEntity entity = toEntity(dto);
        entity.setBuiltIn(false);
        entity.setDataStatus(BasicConstant.DataStatus.ENABLED.getCode());
        roleMapper.insert(entity);
        return entity.getId();
    }

    @IgnoreTenant
    @Override
    public Long createTenantAdminRole(Long tenantId, String tenantName) {
        String roleCode = BasicConstant.SystemRole.TENANT_ADMIN.getCode();
        ensureTenantRoleCodeAvailable(tenantId, roleCode);
        SystemRoleEntity entity = new SystemRoleEntity();
        entity.setTenantId(tenantId);
        entity.setBuiltIn(true);
        entity.setRoleName(tenantName + "管理员");
        entity.setRoleCode(roleCode);
        entity.setRoleDesc(tenantName + "租户管理员，拥有租户内全部数据权限");
        entity.setPermissionType(BasicConstant.PermissionType.ALL.getCode());
        entity.setDataStatus(BasicConstant.DataStatus.ENABLED.getCode());
        if (roleMapper.insert(entity) <= 0) {
            throw new ApiRuntimeException("租户管理员角色创建失败。");
        }
        return entity.getId();
    }

    @Override
    public boolean update(Long id, SystemRoleSaveDTO dto) {
        requireWritable(id, "内置角色不允许修改");
        ensureCodeAvailable(dto.getRoleCode(), id);
        SystemRoleEntity entity = toEntity(dto);
        entity.setId(id);
        return roleMapper.updateById(entity) > 0;
    }

    @Override
    public boolean delete(Long id) {
        requireWritable(id, "内置角色不允许删除");
        return roleMapper.deleteById(id) > 0;
    }

    @Override
    public boolean switchStatus(Long id, Integer status) {
        requireWritable(id, "内置角色不允许修改启用状态");
        SystemRoleEntity entity = new SystemRoleEntity();
        entity.setId(id);
        entity.setDataStatus(status);
        return roleMapper.updateById(entity) > 0;
    }

    private void ensureCodeAvailable(String roleCode, Long excludedId) {
        LambdaQueryWrapper<SystemRoleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemRoleEntity::getRoleCode, roleCode)
                .ne(excludedId != null, SystemRoleEntity::getId, excludedId);
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new ApiRuntimeException("当前租户下角色编码已存在。");
        }
    }

    private void ensureTenantRoleCodeAvailable(Long tenantId, String roleCode) {
        LambdaQueryWrapper<SystemRoleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemRoleEntity::getTenantId, tenantId)
                .eq(SystemRoleEntity::getRoleCode, roleCode);
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new ApiRuntimeException("当前租户下角色编码已存在。");
        }
    }

    private SystemRoleEntity requireRole(Long id) {
        SystemRoleEntity entity = roleMapper.selectById(id);
        if (entity == null) {
            throw new ApiRuntimeException("角色不存在。");
        }
        return entity;
    }

    private boolean isProtectedAdminRole(SystemRoleEntity role) {
        String roleCode = role.getRoleCode();
        return BasicConstant.SystemRole.PLATFORM_ADMIN.getCode().equalsIgnoreCase(roleCode)
                || BasicConstant.SystemRole.TENANT_ADMIN.getCode().equalsIgnoreCase(roleCode);
    }

    private SystemRoleEntity toEntity(SystemRoleSaveDTO dto) {
        SystemRoleEntity entity = new SystemRoleEntity();
        entity.setRoleName(dto.getRoleName());
        entity.setRoleCode(dto.getRoleCode());
        entity.setRoleDesc(dto.getRoleDesc());
        entity.setPermissionType(dto.getPermissionType());
        return entity;
    }

    private SystemRoleVO toVO(SystemRoleEntity entity) {
        if (entity == null) {
            return null;
        }
        SystemRoleVO vo = new SystemRoleVO();
        vo.setId(entity.getId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setDataStatus(entity.getDataStatus());
        vo.setBuiltIn(entity.getBuiltIn());
        vo.setRoleName(entity.getRoleName());
        vo.setRoleCode(entity.getRoleCode());
        vo.setRoleDesc(entity.getRoleDesc());
        vo.setPermissionType(entity.getPermissionType());
        return vo;
    }
}
