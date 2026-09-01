package org.etd.upms.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.commons.lang3.StringUtils;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.UserDetails;
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
import java.util.Objects;
import java.util.Set;

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
    public IPage<SystemTenantVO> page(IPage<SystemTenantEntity> page, List<String> times, String keyword) {
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

        return systemTenantMapper.selectPage(page, wrapper)
                .convert(Mappers.getMapper(SystemTenantConvert.class)::toVo);
    }

    @Override
    public Long insert(SystemTenantEntity entity) {
        ensureTenantAvailable(entity, null);
        if (systemTenantMapper.insert(entity) <= 0) {
            throw new ApiRuntimeException("租户信息创建失败。");
        }
        return entity.getId();
    }

    @Override
    public boolean bindAdminUser(Long tenantId, Long adminUserId) {
        requireExists(tenantId);
        SystemTenantEntity entity = new SystemTenantEntity();
        entity.setId(tenantId);
        entity.setTenantAdminUser(adminUserId);
        return systemTenantMapper.updateById(entity) > 0;
    }

    @Override
    public boolean update(Long tenantId, SystemTenantEntity entity) {
        requireExists(tenantId);
        ensureTenantAvailable(entity, tenantId);
        entity.setId(tenantId);
        return systemTenantMapper.updateById(entity) > 0;
    }

    @Override
    public boolean switchStatus(Long tenantId, Integer status) {
        requireExists(tenantId);
        SystemTenantEntity entity = new SystemTenantEntity();
        entity.setId(tenantId);
        entity.setDataStatus(status);
        return systemTenantMapper.updateById(entity) > 0;
    }

    @Override
    public boolean switchLocked(Long tenantId, boolean locked) {
        requireExists(tenantId);
        SystemTenantEntity entity = new SystemTenantEntity();
        entity.setId(tenantId);
        entity.setLocked(locked);
        return systemTenantMapper.updateById(entity) > 0;
    }

    @Override
    public boolean delete(Long tenantId) {
        requireExists(tenantId);
        return systemTenantMapper.deleteById(tenantId) > 0;
    }

    @Override
    public boolean isLoginEnabled(Long tenantId) {
        if (tenantId == null) {
            return false;
        }
        SystemTenantEntity tenant = systemTenantMapper.selectById(tenantId);
        // TODO(租户安全锁定): locked=true 仍允许登录；权限模块完善时需传递该状态，统一禁止写操作并保留查询权限。
        return tenant != null
                && Objects.equals(BasicConstant.DataStatus.ENABLED.getCode(), tenant.getDataStatus());
    }

    @Override
    public void requireOrdinary(Long tenantId) {
        SystemTenantEntity tenant = requireExists(tenantId);
        if (!Objects.equals(BasicConstant.TenantType.ORDINARY.getCode(), tenant.getTenantType())) {
            throw new ApiRuntimeException("只允许为普通租户设置菜单。");
        }
    }

    private SystemTenantEntity requireExists(Long tenantId) {
        if (tenantId == null) {
            throw new ApiRuntimeException("用户未绑定租户。");
        }
        SystemTenantEntity tenant = systemTenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ApiRuntimeException("租户不存在或已删除。");
        }
        return tenant;
    }

    private void ensureTenantAvailable(SystemTenantEntity tenant, Long excludedTenantId) {
        EtdLambdaQueryWrapper<SystemTenantEntity> wrapper = new EtdLambdaQueryWrapper<>();
        wrapper.ne(excludedTenantId != null, SystemTenantEntity::getId, excludedTenantId)
                .and(query -> query.eq(SystemTenantEntity::getTenantName, tenant.getTenantName())
                .or(StringUtils.isNotBlank(tenant.getCreditCode()),
                        creditQuery -> creditQuery.eq(SystemTenantEntity::getCreditCode, tenant.getCreditCode())));
        if (systemTenantMapper.selectCount(wrapper) > 0) {
            throw new ApiRuntimeException("租户名称或统一社会信用代码已存在。");
        }
    }
}
