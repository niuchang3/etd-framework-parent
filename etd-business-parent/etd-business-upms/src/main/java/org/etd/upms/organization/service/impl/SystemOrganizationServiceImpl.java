package org.etd.upms.organization.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.organization.controller.dto.SystemOrganizationSaveDTO;
import org.etd.upms.organization.controller.vo.SystemOrganizationVO;
import org.etd.upms.organization.entity.SystemOrganizationEntity;
import org.etd.upms.organization.mapper.SystemOrganizationMapper;
import org.etd.upms.organization.service.SystemOrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SystemOrganizationServiceImpl implements SystemOrganizationService {

    @Autowired
    private SystemOrganizationMapper organizationMapper;

    @Override
    public List<SystemOrganizationVO> selectList(Boolean enabled) {
        LambdaQueryWrapper<SystemOrganizationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(enabled != null, SystemOrganizationEntity::getEnabled, enabled)
                .orderByAsc(SystemOrganizationEntity::getSort, SystemOrganizationEntity::getId);
        return organizationMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public SystemOrganizationVO selectById(Long id) {
        return toVO(organizationMapper.selectById(id));
    }

    @Override
    public SystemOrganizationVO requireExists(Long id) {
        SystemOrganizationVO organization = selectById(id);
        if (organization == null) {
            throw new ApiRuntimeException("组织机构不存在。");
        }
        return organization;
    }

    @Override
    public void requireAllExist(Set<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SystemOrganizationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemOrganizationEntity::getId, ids);
        if (organizationMapper.selectCount(wrapper) != ids.size()) {
            throw new ApiRuntimeException("只能选择当前租户下存在的组织机构。");
        }
    }

    @Override
    public Long insert(SystemOrganizationSaveDTO dto, String parentIdPath) {
        ensureCodeAvailable(dto.getOrgCode(), null);
        SystemOrganizationEntity entity = toEntity(dto, parentIdPath);
        entity.setEnabled(true);
        organizationMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public boolean update(Long id, SystemOrganizationSaveDTO dto, String parentIdPath) {
        requireExists(id);
        ensureCodeAvailable(dto.getOrgCode(), id);
        SystemOrganizationEntity entity = toEntity(dto, parentIdPath);
        entity.setId(id);
        return organizationMapper.updateById(entity) > 0;
    }

    @Override
    public void replaceDescendantPathPrefix(String oldPrefix, String newPrefix) {
        List<SystemOrganizationEntity> descendants = selectDescendants(oldPrefix);
        descendants.forEach(entity -> updateDescendantPath(entity, oldPrefix, newPrefix));
    }

    @Override
    public boolean existsChild(Long id) {
        LambdaQueryWrapper<SystemOrganizationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemOrganizationEntity::getParentId, id);
        return organizationMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsReference(Long id) {
        return organizationMapper.selectUserReferenceCount(id) > 0
                || organizationMapper.selectRoleReferenceCount(id) > 0;
    }

    @Override
    public boolean delete(Long id) {
        requireExists(id);
        return organizationMapper.deleteById(id) > 0;
    }

    @Override
    public Set<Long> selectSubtreeIds(Long id) {
        SystemOrganizationVO organization = requireExists(id);
        String pathPrefix = organization.getParentIdPath() + id + "/";
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(id);
        selectDescendants(pathPrefix).forEach(descendant -> ids.add(descendant.getId()));
        return ids;
    }

    @Override
    public boolean deleteByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<SystemOrganizationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemOrganizationEntity::getId, ids);
        return organizationMapper.delete(wrapper) == ids.size();
    }

    @Override
    public boolean switchEnabled(Long id, Boolean enabled) {
        requireExists(id);
        SystemOrganizationEntity entity = new SystemOrganizationEntity();
        entity.setId(id);
        entity.setEnabled(enabled);
        return organizationMapper.updateById(entity) > 0;
    }

    private List<SystemOrganizationEntity> selectDescendants(String pathPrefix) {
        LambdaQueryWrapper<SystemOrganizationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(SystemOrganizationEntity::getParentIdPath, pathPrefix)
                .orderByAsc(SystemOrganizationEntity::getParentIdPath);
        return organizationMapper.selectList(wrapper);
    }

    private void updateDescendantPath(SystemOrganizationEntity entity, String oldPrefix, String newPrefix) {
        String suffix = entity.getParentIdPath().substring(oldPrefix.length());
        SystemOrganizationEntity update = new SystemOrganizationEntity();
        update.setId(entity.getId());
        update.setParentIdPath(newPrefix + suffix);
        organizationMapper.updateById(update);
    }

    private void ensureCodeAvailable(String orgCode, Long excludedId) {
        LambdaQueryWrapper<SystemOrganizationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemOrganizationEntity::getOrgCode, orgCode)
                .ne(excludedId != null, SystemOrganizationEntity::getId, excludedId);
        if (organizationMapper.selectCount(wrapper) > 0) {
            throw new ApiRuntimeException("当前租户下组织编码已存在。");
        }
    }

    private SystemOrganizationEntity toEntity(SystemOrganizationSaveDTO dto, String parentIdPath) {
        SystemOrganizationEntity entity = new SystemOrganizationEntity();
        entity.setParentId(dto.getParentId());
        entity.setParentIdPath(parentIdPath);
        entity.setOrgCode(dto.getOrgCode());
        entity.setOrgName(dto.getOrgName());
        entity.setOrgType(dto.getOrgType());
        entity.setLeaderUserId(dto.getLeaderUserId());
        entity.setSort(dto.getSort());
        return entity;
    }

    private SystemOrganizationVO toVO(SystemOrganizationEntity entity) {
        if (entity == null) {
            return null;
        }
        SystemOrganizationVO vo = new SystemOrganizationVO();
        vo.setId(entity.getId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setParentId(entity.getParentId());
        vo.setParentIdPath(entity.getParentIdPath());
        vo.setOrgCode(entity.getOrgCode());
        vo.setOrgName(entity.getOrgName());
        vo.setOrgType(entity.getOrgType());
        vo.setLeaderUserId(entity.getLeaderUserId());
        vo.setSort(entity.getSort());
        vo.setEnabled(entity.getEnabled());
        return vo;
    }
}
