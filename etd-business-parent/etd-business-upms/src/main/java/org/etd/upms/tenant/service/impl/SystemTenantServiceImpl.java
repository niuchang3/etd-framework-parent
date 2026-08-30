package org.etd.upms.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.framework.common.core.user.UserDetails;
import org.apache.commons.lang3.StringUtils;
import org.etd.upms.tenant.converter.SystemTenantConvert;
import org.etd.upms.tenant.entity.SystemTenantEntity;
import org.etd.upms.tenant.mapper.SystemTenantMapper;
import org.etd.upms.tenant.service.SystemTenantService;
import org.etd.upms.tenant.controller.vo.SystemTenantVO;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.mybaits.core.EtdLambdaQueryWrapper;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SystemTenantServiceImpl implements SystemTenantService {

    @Autowired
    private SystemTenantMapper systemTenantMapper;

    @Override
    public List<SystemTenantVO> selectAll() {
        List<SystemTenantEntity> entities = systemTenantMapper.selectList(new QueryWrapper<>());
        return Mappers.getMapper(SystemTenantConvert.class).toVo(entities);
    }

    @Override
    public List<SystemTenantVO> selectByIds(Set<Long> tenantIds) {
        EtdLambdaQueryWrapper<SystemTenantEntity> wrapper = new EtdLambdaQueryWrapper<>();
        wrapper.in(SystemTenantEntity::getId, tenantIds);
        List<SystemTenantEntity> entities = systemTenantMapper.selectList(wrapper);
        return Mappers.getMapper(SystemTenantConvert.class).toVo(entities);
    }

    @Override
    public SystemTenantVO selectCurrentTenant() {
        EtdLambdaQueryWrapper<SystemTenantEntity> wrapper = new EtdLambdaQueryWrapper<>();
        wrapper.eq(SystemTenantEntity::getId, RequestContext.getTenantCode());
        SystemTenantEntity systemTenantEntity = systemTenantMapper.selectOne(wrapper);
        return Mappers.getMapper(SystemTenantConvert.class).toVo(systemTenantEntity);
    }


    @Override
    public IPage<SystemTenantVO> page(IPage page, List<String> times, String keyword) {
        UserDetails user = RequestContext.getUser();
        EtdLambdaQueryWrapper<SystemTenantEntity> wrapper = new EtdLambdaQueryWrapper<>();
        if (!user.isPlatformAdmin()) {
            wrapper.eq(SystemTenantEntity::getId, user.getTenantId());
        }
        if(!ObjectUtils.isEmpty(times)){
            wrapper.between(SystemTenantEntity::getCreateTime,times.get(0),times.get(1));
        }
        if(!StringUtils.isEmpty(keyword)){
            wrapper.and((queryWrapper) -> {
                queryWrapper.like(SystemTenantEntity::getTenantName, keyword)
                        .or()
                        .like(SystemTenantEntity::getDescription, keyword)
                        .or().
                        like(SystemTenantEntity::getCreditCode,keyword);
            });
        }

        IPage iPage = systemTenantMapper.selectPage(page, wrapper);
        List<SystemTenantVO> vos = Mappers.getMapper(SystemTenantConvert.class).toVo(iPage.getRecords());
        iPage.setRecords(vos);
        return iPage;
    }

    @Override
    public boolean switchLocked(Long id, Boolean status) {
        SystemTenantEntity entity = new SystemTenantEntity();
        entity.setId(id);
        entity.setLocked(status);
        return systemTenantMapper.updateById(entity) > 0;
    }

    @Override
    public boolean appendMenu(Long tenantId, Long menuId) {
        SystemTenantEntity tenant = selectTenantForUpdate(tenantId);
        if (tenant == null) {
            return false;
        }
        Set<String> menuIds = parseMenuIds(tenant.getMenus());
        menuIds.add(menuId.toString());
        LambdaUpdateWrapper<SystemTenantEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SystemTenantEntity::getId, tenantId)
                .set(SystemTenantEntity::getMenus, String.join(",", menuIds));
        return systemTenantMapper.update(null, updateWrapper) > 0;
    }

    @Override
    public boolean removeMenus(Long tenantId, Set<Long> menuIds) {
        SystemTenantEntity tenant = selectTenantForUpdate(tenantId);
        if (tenant == null) {
            return false;
        }
        Set<String> retainedMenuIds = parseMenuIds(tenant.getMenus());
        Set<String> removedMenuIds = menuIds.stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());
        retainedMenuIds.removeAll(removedMenuIds);
        LambdaUpdateWrapper<SystemTenantEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SystemTenantEntity::getId, tenantId)
                .set(SystemTenantEntity::getMenus, String.join(",", retainedMenuIds));
        return systemTenantMapper.update(null, updateWrapper) > 0;
    }

    private SystemTenantEntity selectTenantForUpdate(Long tenantId) {
        EtdLambdaQueryWrapper<SystemTenantEntity> wrapper = new EtdLambdaQueryWrapper<>();
        wrapper.eq(SystemTenantEntity::getId, tenantId).last("FOR UPDATE");
        return systemTenantMapper.selectOne(wrapper);
    }

    private Set<String> parseMenuIds(String menus) {
        if (StringUtils.isBlank(menus)) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(menus.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public boolean insert() {
        return false;
    }

}
