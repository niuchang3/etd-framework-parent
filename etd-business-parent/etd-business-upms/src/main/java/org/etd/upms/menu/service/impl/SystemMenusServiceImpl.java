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

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SystemMenusServiceImpl implements SystemMenusService {


    @Autowired
    private SystemMenusMapper systemMenusMapper;

    /**
     * 查询 By Id
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @Override
    public SystemMenuVO selectById(Long id) {
        SystemMenusEntity entity = systemMenusMapper.selectById(id);
        return Mappers.getMapper(SystemMenusConverter.class).toMenuVO(entity);
    }

    /**
     * 新增保存
     *
     * @param dto 参数 dto
     * @return 处理结果
     */
    @Override
    public Long insert(SystemMenuSaveDTO dto) {
        SystemMenusEntity entity = Mappers.getMapper(SystemMenusConverter.class).toEntity(dto);
        entity.setCreateTime(Instant.now());
        entity.setDataStatus(BasicConstant.DataStatus.ENABLED.getCode());
        systemMenusMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 更新修改
     *
     * @param id 参数 id
     * @param dto 参数 dto
     * @return 处理结果
     */
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

    /**
     * 查询 Subtree Ids
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @Override
    public Set<Long> selectSubtreeIds(Long id) {
        if (systemMenusMapper.selectById(id) == null) {
            return Set.of();
        }
        return collectDescendantIds(id);
    }

    /**
     * 删除 By Ids
     *
     * @param ids 参数 ids
     * @return 处理结果
     */
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
        return collectDescendantIdsFromMenus(rootId, menus);
    }

    /**
     * 使用父菜单 ID 分组建立邻接表，基于队列广度优先遍历收集所有子节点。
     *
     * @param rootId 根菜单 ID
     * @param menus  菜单全量列表
     * @return 包含根菜单及所有子孙菜单 ID 的集合
     */
    private Set<Long> collectDescendantIdsFromMenus(Long rootId, List<SystemMenusEntity> menus) {
        Map<Long, List<Long>> parentToChildrenMap = menus.stream()
                .filter(menu -> menu.getParentId() != null)
                .collect(Collectors.groupingBy(
                        SystemMenusEntity::getParentId,
                        Collectors.mapping(SystemMenusEntity::getId, Collectors.toList())
                ));

        Set<Long> result = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        result.add(rootId);

        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            List<Long> children = parentToChildrenMap.get(currentId);
            if (children != null) {
                for (Long childId : children) {
                    if (result.add(childId)) {
                        queue.add(childId);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 切换 Status
     *
     * @param id 参数 id
     * @param status 参数 status
     * @return 处理结果
     */
    @Override
    public boolean switchStatus(Long id, Integer status) {
        LambdaUpdateWrapper<SystemMenusEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SystemMenusEntity::getId, id)
                .set(SystemMenusEntity::getDataStatus, status);
        return systemMenusMapper.update(null, wrapper) > 0;
    }

    /**
     * 查询 All Enabled
     *
     * @return 处理结果
     */
    @Override
    public List<SystemMenuVO> selectAllEnabled() {
        LambdaQueryWrapper<SystemMenusEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemMenusEntity::getDataStatus, BasicConstant.DataStatus.ENABLED.getCode())
                .orderByAsc(SystemMenusEntity::getSort, SystemMenusEntity::getId);
        return Mappers.getMapper(SystemMenusConverter.class).toMenuVO(systemMenusMapper.selectList(wrapper));
    }

    /**
     * 查询 Enabled By Ids
     *
     * @param menuIds 参数 menuIds
     * @param tenantId 参数 tenantId
     * @return 处理结果
     */
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
