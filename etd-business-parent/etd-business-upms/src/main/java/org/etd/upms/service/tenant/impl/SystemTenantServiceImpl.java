package org.etd.upms.service.tenant.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.TenantAuthority;
import com.etd.framework.starter.client.core.user.UserDetails;
import org.apache.commons.lang3.StringUtils;
import org.etd.upms.converter.SystemTenantConvert;
import org.etd.upms.entity.SystemTenantEntity;
import org.etd.upms.mapper.tenant.SystemTenantMapper;
import org.etd.upms.service.tenant.SystemTenantService;
import org.etd.upms.controller.tenant.vo.SystemTenantVO;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.mybaits.core.EtdLambdaQueryWrapper;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
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
            Set<Long> tenetIds = user.getAuthorities().stream().map(TenantAuthority::getTenantId).collect(Collectors.toSet());
            wrapper.in(SystemTenantEntity::getId, tenetIds);
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
    public boolean insert() {
        return false;
    }

}
