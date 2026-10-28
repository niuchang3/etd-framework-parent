package org.etd.ai.agent.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.etd.ai.agent.constant.AgentPolicy;
import org.etd.ai.agent.entity.AgentEntity;
import org.etd.ai.agent.mapper.AgentMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermissionTable;
import org.etd.framework.starter.mybaits.permission.model.DataPermissionScope;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

/** Agent基础能力；公开可读与角色范围内维护分离，事务编排由Biz负责。 */
@Service
@RequiredArgsConstructor
@DataPermission(tables = @DataPermissionTable(table = "ai_agent", publicColumn = AgentPolicy.PUBLIC_COLUMN))
public class AgentService {
    private final AgentMapper mapper;

    /** @param current 页码 @param size 页大小 @param keyword 名称 @param dataStatus 启停状态 @param published 是否已发布 @return 可见卡片 */
    public IPage<AgentEntity> fetchAgentPage(long current, long size, String keyword,
            Integer dataStatus, Boolean published) {
        Page<AgentEntity> page = new Page<AgentEntity>(current, size);
        IPage<AgentEntity> result = mapper.selectReadablePage(page, keyword, dataStatus, published);
        for (AgentEntity agent : result.getRecords()) {
            populateWritable(agent);
        }
        return result;
    }

    /** @param id Agent ID @param write 是否要求维护资格 @param lock 是否加行锁 @return 授权记录 @throws ApiRuntimeException 记录不可访问或无维护权限 */
    public AgentEntity requireAgent(Long id, boolean write, boolean lock) {
        AgentEntity agent = mapper.selectAccessible(id, lock);
        if (agent == null) {
            throw new ApiRuntimeException("Agent不存在或无操作权限");
        }
        populateWritable(agent);
        if (write && !Boolean.TRUE.equals(agent.getWritable())) {
            throw new ApiRuntimeException("Agent不存在或无操作权限");
        }
        return agent;
    }

    /** @param entity 用户可编辑信息 @return 新记录ID @throws ApiRuntimeException 无所选组织新增权限 */
    public Long createAgent(AgentEntity entity) {
        DataPermissionScope scope = DataPermissionScope.current();
        if (!scope.canManage(entity.getOrgId(), scope.userId())) {
            throw new ApiRuntimeException("无Agent新增数据权限");
        }
        mapper.insert(entity);
        return entity.getId();
    }

    /** @param entity 已加锁且版本匹配的实体 @throws ApiRuntimeException 更新发生版本冲突 */
    @DataPermission(tables = @DataPermissionTable(table = "ai_agent"))
    public void modifyAgent(AgentEntity entity) {
        // 可空字段显式清空；不修改全局字段更新策略，也不拼接setSql。
        LambdaUpdateWrapper<AgentEntity> changes = Wrappers.<AgentEntity>lambdaUpdate()
                .eq(AgentEntity::getId, entity.getId())
                .set(entity.getDescription() == null, AgentEntity::getDescription, null)
                .set(entity.getAvatarPath() == null, AgentEntity::getAvatarPath, null);
        entity.setUpdateTime(null);
        entity.setUpdateBy(null);
        if (mapper.update(entity, changes) != 1) {
            throw new ApiRuntimeException("Agent已变化，请刷新后重试");
        }
    }

    /** @param id 已加锁并校验维护资格的Agent ID @throws ApiRuntimeException 删除失败 */
    @DataPermission(tables = @DataPermissionTable(table = "ai_agent"))
    public void removeAgent(Long id) {
        if (mapper.deleteById(id) != 1) {
            throw new ApiRuntimeException("Agent已变化，请刷新后重试");
        }
    }

    /** 公开读取只用于可见性，页面维护能力复用已有角色数据范围。 */
    private void populateWritable(AgentEntity agent) {
        DataPermissionScope scope = DataPermissionScope.current();
        agent.setWritable(scope.canManage(agent.getOrgId(), agent.getCreateBy()));
    }
}
