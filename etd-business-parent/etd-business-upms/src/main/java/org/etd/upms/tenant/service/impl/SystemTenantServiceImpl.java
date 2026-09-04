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
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * 租户基础能力 Service 实现类。
 */
@Service
public class SystemTenantServiceImpl implements SystemTenantService {

    @Autowired
    private SystemTenantMapper systemTenantMapper;

    /**
     * 查询 All
     *
     * @return 处理结果
     */
    @Override
    public List<SystemTenantVO> selectAll() {
        List<SystemTenantEntity> entities = systemTenantMapper.selectList(new QueryWrapper<>());
        return Mappers.getMapper(SystemTenantConvert.class).toVo(entities);
    }

    /**
     * 查询 By Ids
     *
     * @param tenantIds 参数 tenantIds
     * @return 处理结果
     */
    @Override
    public List<SystemTenantVO> selectByIds(Set<Long> tenantIds) {
        EtdLambdaQueryWrapper<SystemTenantEntity> wrapper = new EtdLambdaQueryWrapper<>();
        wrapper.in(SystemTenantEntity::getId, tenantIds);
        List<SystemTenantEntity> entities = systemTenantMapper.selectList(wrapper);
        return Mappers.getMapper(SystemTenantConvert.class).toVo(entities);
    }

    /**
     * 查询 Current Tenant
     *
     * @return 处理结果
     */
    @Override
    public SystemTenantVO selectCurrentTenant() {
        EtdLambdaQueryWrapper<SystemTenantEntity> wrapper = new EtdLambdaQueryWrapper<>();
        wrapper.eq(SystemTenantEntity::getId, RequestContext.getTenantCode());
        SystemTenantEntity systemTenantEntity = systemTenantMapper.selectOne(wrapper);
        return Mappers.getMapper(SystemTenantConvert.class).toVo(systemTenantEntity);
    }


    /**
     * 分页查询
     *
     * @param page 参数 page
     * @param times 参数 times
     * @param keyword 参数 keyword
     * @return 处理结果
     */
    @Override
    public IPage<SystemTenantVO> page(IPage<SystemTenantEntity> page, List<Instant> times, String keyword) {
        EtdLambdaQueryWrapper<SystemTenantEntity> wrapper = new EtdLambdaQueryWrapper<>();
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

    /**
     * 新增保存
     *
     * @param entity 参数 entity
     * @return 处理结果
     */
    @Override
    public Long insert(SystemTenantEntity entity) {
        ensureTenantAvailable(entity, null);
        if (systemTenantMapper.insert(entity) <= 0) {
            throw new ApiRuntimeException("租户信息创建失败。");
        }
        return entity.getId();
    }

    /**
     * bind Admin User
     *
     * @param tenantId 参数 tenantId
     * @param adminUserId 参数 adminUserId
     * @return 处理结果
     */
    @Override
    public boolean bindAdminUser(Long tenantId, Long adminUserId) {
        requireExists(tenantId);
        SystemTenantEntity entity = new SystemTenantEntity();
        entity.setId(tenantId);
        entity.setTenantAdminUser(adminUserId);
        return systemTenantMapper.updateById(entity) > 0;
    }

    /**
     * 更新修改
     *
     * @param tenantId 参数 tenantId
     * @param entity 参数 entity
     * @return 处理结果
     */
    @Override
    public boolean update(Long tenantId, SystemTenantEntity entity) {
        requireExists(tenantId);
        ensureTenantAvailable(entity, tenantId);
        entity.setId(tenantId);
        return systemTenantMapper.updateById(entity) > 0;
    }

    /**
     * 切换 Status
     *
     * @param tenantId 参数 tenantId
     * @param status 参数 status
     * @return 处理结果
     */
    @Override
    public boolean switchStatus(Long tenantId, Integer status) {
        requireExists(tenantId);
        SystemTenantEntity entity = new SystemTenantEntity();
        entity.setId(tenantId);
        entity.setDataStatus(status);
        return systemTenantMapper.updateById(entity) > 0;
    }

    /**
     * 切换 Locked
     *
     * @param tenantId 参数 tenantId
     * @param locked 参数 locked
     * @return 处理结果
     */
    @Override
    public boolean switchLocked(Long tenantId, boolean locked) {
        requireExists(tenantId);
        SystemTenantEntity entity = new SystemTenantEntity();
        entity.setId(tenantId);
        entity.setLocked(locked);
        return systemTenantMapper.updateById(entity) > 0;
    }

    /**
     * 删除
     *
     * @param tenantId 参数 tenantId
     * @return 处理结果
     */
    @Override
    public boolean delete(Long tenantId) {
        requireExists(tenantId);
        return systemTenantMapper.deleteById(tenantId) > 0;
    }

    /**
     * 按租户标识获取租户实体。
     *
     * @param tenantId 租户标识
     * @return 租户实体，不存在时返回 null
     */
    @Override
    public SystemTenantEntity fetchById(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        return systemTenantMapper.selectById(tenantId);
    }

    /**
     * 校验并要求 Ordinary
     *
     * @param tenantId 参数 tenantId
     */
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
