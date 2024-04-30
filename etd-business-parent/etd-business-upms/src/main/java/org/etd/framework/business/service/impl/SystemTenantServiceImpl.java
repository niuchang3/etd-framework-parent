package org.etd.framework.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.TenantAuthority;
import com.etd.framework.starter.client.core.user.UserDetails;
import org.etd.framework.business.converter.SystemTenantConvert;
import org.etd.framework.business.entity.SystemTenantEntity;
import org.etd.framework.business.entity.SystemUserEntity;
import org.etd.framework.business.mapper.SystemTenantMapper;
import org.etd.framework.business.service.SystemTenantService;
import org.etd.framework.business.service.SystemUserRoleRelService;
import org.etd.framework.business.service.SystemUserService;
import org.etd.framework.business.vo.SystemTenantVO;
import org.etd.framework.business.vo.SystemUserRoleVO;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.core.EtdLambdaQueryWrapper;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SystemTenantServiceImpl implements SystemTenantService {

    @Autowired
    private SystemTenantMapper systemTenantMapper;

    @Autowired
    private SystemUserRoleRelService userRoleRelService;

    @Autowired
    private SystemUserService userService;

    @Override
    public List<SystemTenantVO> selectByUser(UserDetails userDetails) {
        if (ObjectUtils.isEmpty(userDetails)) {
            throw new ApiRuntimeException("该请求需要身份认证。");
        }
        if (userDetails.isPlatformAdmin()) {
            return selectAll();
        }


        List<SystemUserRoleVO> roleVOS = userRoleRelService.selectByUser(userDetails.getId());

        if (CollectionUtils.isEmpty(roleVOS)) {
            throw new ApiRuntimeException("用户身份信息异常,请联系管理员处理");
        }
        List<Long> tenantIds = roleVOS.stream()
                .map(SystemUserRoleVO::getTenantId)
                .collect(Collectors.toList());

        EtdLambdaQueryWrapper<SystemTenantEntity> wrapper = new EtdLambdaQueryWrapper<>();
        wrapper.in(SystemTenantEntity::getId, tenantIds);

        List<SystemTenantEntity> entities = systemTenantMapper.selectList(wrapper);
        return Mappers.getMapper(SystemTenantConvert.class).toVo(entities);
    }

    private List<SystemTenantVO> selectAll() {
        List<SystemTenantEntity> entities = systemTenantMapper.selectList(new QueryWrapper<>());
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
        wrapper.and((queryWrapper) -> {
            queryWrapper.like(SystemTenantEntity::getTenantName, keyword)
                    .or()
                    .like(SystemTenantEntity::getDescription, keyword);
        });


        IPage iPage = systemTenantMapper.selectPage(page, wrapper);
        List<SystemTenantVO> vos = Mappers.getMapper(SystemTenantConvert.class).toVo(iPage.getRecords());
        populateUser(vos);
        iPage.setRecords(vos);
        return iPage;
    }

    /**
     * 填充用户信息
     * @param vos
     */
    private void populateUser(List<SystemTenantVO> vos){
        Set<Long> adminIds = vos.stream().map(SystemTenantVO::getTenantAdminUser).collect(Collectors.toSet());
        List<SystemUserEntity> users = userService.selectByUserById(adminIds);
        Map<Long, SystemUserEntity> userEntityMap = users.stream().collect(Collectors.toMap(SystemUserEntity::getId, Function.identity()));
        for (SystemTenantVO vo : vos) {
            if (!userEntityMap.containsKey(vo.getTenantAdminUser())) {
                continue;
            }
            vo.setAdminUser(userEntityMap.get(vo.getTenantAdminUser()).getUserName());
        }
    }
}
