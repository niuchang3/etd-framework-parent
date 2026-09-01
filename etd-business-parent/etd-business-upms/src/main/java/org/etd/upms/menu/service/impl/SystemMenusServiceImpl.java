package org.etd.upms.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.etd.upms.menu.controller.dto.SystemMenuSaveDTO;
import org.etd.upms.menu.controller.vo.SystemMenuVO;
import org.etd.upms.menu.converter.SystemMenusConverter;
import org.etd.upms.menu.entity.SystemMenusEntity;
import org.etd.upms.menu.mapper.SystemMenusMapper;
import org.etd.upms.menu.service.SystemMenusService;
import org.etd.upms.user.controller.vo.SystemUserMenusVO;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SystemMenusServiceImpl implements SystemMenusService {


    @Autowired
    private SystemMenusMapper systemMenusMapper;

    @Override
    public SystemMenuVO selectById(Long id) {
        SystemMenusEntity entity = systemMenusMapper.selectById(id);
        return Mappers.getMapper(SystemMenusConverter.class).toMenuVO(entity);
    }

    @Override
    public Long insert(SystemMenuSaveDTO dto) {
        SystemMenusEntity entity = Mappers.getMapper(SystemMenusConverter.class).toEntity(dto);
        entity.setCreateTime(new Date());
        entity.setDataStatus(BasicConstant.DataStatus.ENABLED.getCode());
        systemMenusMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public boolean update(Long id, SystemMenuSaveDTO dto) {
        LambdaUpdateWrapper<SystemMenusEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SystemMenusEntity::getId, id)
                .set(SystemMenusEntity::getParentId, dto.getParentId())
                .set(SystemMenusEntity::getMenuName, dto.getMenuName())
                .set(SystemMenusEntity::getMenuPath, dto.getMenuPath())
                .set(SystemMenusEntity::getMenuRouter, dto.getMenuRouter())
                .set(SystemMenusEntity::getMenuIcon, dto.getMenuIcon())
                .set(SystemMenusEntity::getMenuType, dto.getMenuType())
                .set(SystemMenusEntity::getSort, dto.getSort());
        return systemMenusMapper.update(null, wrapper) > 0;
    }

    @Override
    public Set<Long> selectSubtreeIds(Long id) {
        if (systemMenusMapper.selectById(id) == null) {
            return Set.of();
        }
        return collectDescendantIds(id);
    }

    @IgnoreTenant
    @Override
    public boolean deleteByIds(Set<Long> ids) {
        return systemMenusMapper.deleteByIds(ids) == ids.size();
    }

    /**
     * 一次读取菜单关系后计算完整子树，避免逐层查询数据库。
     */
    private Set<Long> collectDescendantIds(Long rootId) {
        LambdaQueryWrapper<SystemMenusEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SystemMenusEntity::getId, SystemMenusEntity::getParentId);
        List<SystemMenusEntity> menus = systemMenusMapper.selectList(wrapper);
        Set<Long> menuIds = new LinkedHashSet<>();
        menuIds.add(rootId);
        boolean foundChild;
        do {
            foundChild = menus.stream()
                    .filter(menu -> menuIds.contains(menu.getParentId()))
                    .map(SystemMenusEntity::getId)
                    .filter(menuIds::add)
                    .findAny()
                    .isPresent();
        } while (foundChild);
        return menuIds;
    }

    @Override
    public boolean switchStatus(Long id, Integer status) {
        LambdaUpdateWrapper<SystemMenusEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SystemMenusEntity::getId, id)
                .set(SystemMenusEntity::getDataStatus, status);
        return systemMenusMapper.update(null, wrapper) > 0;
    }

    @Override
    public List<SystemMenuVO> selectAllEnabled() {
        LambdaQueryWrapper<SystemMenusEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemMenusEntity::getDataStatus, BasicConstant.DataStatus.ENABLED.getCode())
                .orderByAsc(SystemMenusEntity::getSort, SystemMenusEntity::getId);
        return Mappers.getMapper(SystemMenusConverter.class).toMenuVO(systemMenusMapper.selectList(wrapper));
    }

    @Override
    public List<SystemUserMenusVO> selectEnabledByIds(Set<Long> menuIds, Long tenantId) {
        if (menuIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<SystemMenusEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemMenusEntity::getId, menuIds)
                .eq(SystemMenusEntity::getDataStatus, BasicConstant.DataStatus.ENABLED.getCode())
                .orderByAsc(SystemMenusEntity::getSort, SystemMenusEntity::getId);
        List<SystemMenusEntity> systemMenus = systemMenusMapper.selectList(wrapper);
        List<SystemUserMenusVO> menus = Mappers.getMapper(SystemMenusConverter.class).toUserMenu(systemMenus);
        menus.forEach(menu -> menu.setTenantId(tenantId));
        return menus;
    }
}
