package org.etd.upms.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.role.controller.dto.SystemRoleSaveDTO;
import org.etd.upms.role.controller.vo.SystemRoleVO;
import org.etd.upms.role.entity.SystemRoleEntity;
import org.etd.upms.role.mapper.SystemRoleMapper;
import org.etd.upms.role.service.SystemRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SystemRoleServiceImpl implements SystemRoleService {

    @Autowired
    private SystemRoleMapper roleMapper;

    @Override
    public IPage<SystemRoleVO> page(long current, long size, String keyword, Integer dataStatus) {
        LambdaQueryWrapper<SystemRoleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dataStatus != null, SystemRoleEntity::getDataStatus, dataStatus)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(SystemRoleEntity::getRoleName, keyword)
                        .or().like(SystemRoleEntity::getRoleCode, keyword))
                .orderByDesc(SystemRoleEntity::getBuiltIn)
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
    public Long insert(SystemRoleSaveDTO dto) {
        ensureCodeAvailable(dto.getRoleCode(), null);
        SystemRoleEntity entity = toEntity(dto);
        entity.setBuiltIn(false);
        entity.setDataStatus(BasicConstant.DataStatus.ENABLED.getCode());
        roleMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public boolean update(Long id, SystemRoleSaveDTO dto) {
        SystemRoleEntity existing = requireRole(id);
        if (Boolean.TRUE.equals(existing.getBuiltIn()) && !existing.getRoleCode().equals(dto.getRoleCode())) {
            throw new ApiRuntimeException("内置角色不允许修改角色编码。");
        }
        ensureCodeAvailable(dto.getRoleCode(), id);
        SystemRoleEntity entity = toEntity(dto);
        entity.setId(id);
        return roleMapper.updateById(entity) > 0;
    }

    @Override
    public boolean delete(Long id) {
        SystemRoleEntity entity = requireRole(id);
        if (Boolean.TRUE.equals(entity.getBuiltIn())) {
            throw new ApiRuntimeException("内置角色不允许删除。");
        }
        return roleMapper.deleteById(id) > 0;
    }

    @Override
    public boolean switchStatus(Long id, Integer status) {
        requireRole(id);
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

    private SystemRoleEntity requireRole(Long id) {
        SystemRoleEntity entity = roleMapper.selectById(id);
        if (entity == null) {
            throw new ApiRuntimeException("角色不存在。");
        }
        return entity;
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
