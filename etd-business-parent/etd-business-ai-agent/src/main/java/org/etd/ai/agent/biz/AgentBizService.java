package org.etd.ai.agent.biz;

import jakarta.validation.Validator;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.etd.ai.agent.converter.AgentConverter;
import org.etd.ai.agent.entity.AgentEntity;
import org.etd.ai.agent.model.AgentConfiguration;
import org.etd.ai.agent.service.AgentService;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.model.constant.ModelType;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.ai.model.service.ModelService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Agent创建、草稿更新、发布、启停和删除编排，不直接访问Mapper或接收Controller DTO。 */
@Service
@RequiredArgsConstructor
public class AgentBizService {
    private final AgentService agents;
    private final ModelConnectionService connections;
    private final ModelService models;
    private final KnowledgeBaseService knowledgeBases;
    private final Validator validator;

    /** @param input 可编辑草稿 @return 新记录及初始版本 @throws ApiRuntimeException 配置或引用不可用 */
    @Transactional
    public AgentEntity createAgent(AgentEntity input) {
        Long tenantId = RequestContext.getTenantCode();
        validateConfiguration(input.getDraftConfig(), tenantId, false);
        input.setName(input.getName().trim());
        agents.createAgent(input);
        return input;
    }

    /** @param id Agent ID @param input 完整草稿 @param version 页面版本 @return 更新后的记录 @throws ApiRuntimeException 无权限、归属变化或版本冲突 */
    @Transactional
    public AgentEntity modifyAgent(Long id, AgentEntity input, Long version) {
        var agent = requireVersion(id, version);
        if (!Objects.equals(agent.getOrgId(), input.getOrgId())) {
            throw new ApiRuntimeException("Agent创建后不允许变更组织归属");
        }
        validateConfiguration(input.getDraftConfig(), agent.getTenantId(), false);
        input.setName(input.getName().trim());
        AgentConverter.INSTANCE.updateEntity(input, agent);
        agents.modifyAgent(agent);
        return agent;
    }

    /** @param id Agent ID @param version 页面版本 @return 发布后的记录 @throws ApiRuntimeException 草稿不完整、引用不可用或版本冲突 */
    @Transactional
    public AgentEntity publishAgent(Long id, Long version) {
        var agent = requireVersion(id, version);
        validateConfiguration(agent.getDraftConfig(), agent.getTenantId(), true);
        var user = RequestContext.getUser();
        if (user == null || user.getId() == null) {
            throw new ApiRuntimeException("发布Agent需要有效登录身份");
        }
        var snapshot = AgentConverter.INSTANCE.copyConfiguration(agent.getDraftConfig());
        agent.setPublishedConfig(snapshot);
        agent.setPublishedTime(Instant.now());
        agent.setPublishedBy(user.getId());
        agents.modifyAgent(agent);
        return agent;
    }

    /** @param id Agent ID @param dataStatus 目标启停状态 @param version 页面版本 @return 最新记录 @throws ApiRuntimeException 状态不合法、无权限或版本冲突 */
    @Transactional
    public AgentEntity modifyAgentStatus(Long id, Integer dataStatus, Long version) {
        if (!Objects.equals(dataStatus, BasicConstant.DataStatus.ENABLED_CODE)
                && !Objects.equals(dataStatus, BasicConstant.DataStatus.DISABLED_CODE)) {
            throw new ApiRuntimeException("不支持的Agent启停状态");
        }
        var agent = requireVersion(id, version);
        agent.setDataStatus(dataStatus);
        agents.modifyAgent(agent);
        return agent;
    }

    /** @param id Agent ID @param version 页面版本 @throws ApiRuntimeException 无维护权限或版本冲突 */
    @Transactional
    public void removeAgent(Long id, Long version) {
        requireVersion(id, version);
        agents.removeAgent(id);
    }

    /** 在同一事务内先锁定记录，再比较页面版本，防止发布或删除更新后的草稿。 */
    private AgentEntity requireVersion(Long id, Long version) {
        var agent = agents.requireAgent(id, true, true);
        if (version == null || !Objects.equals(agent.getVersion(), version)) {
            throw new ApiRuntimeException("Agent已变化，请刷新后重试");
        }
        return agent;
    }

    /** 草稿允许不完整，但已填写的参数及引用必须合法；发布额外要求模型和提示词。 */
    private void validateConfiguration(AgentConfiguration configuration, Long tenantId, boolean publishing) {
        if (configuration == null) {
            throw new ApiRuntimeException("请填写Agent草稿配置");
        }
        var violations = validator.validate(configuration);
        if (!violations.isEmpty()) {
            throw new ApiRuntimeException("Agent配置字段不符合约束，请检查参数范围和文本长度");
        }
        if (publishing && (configuration.getModelConnectionId() == null
                || configuration.getSystemPrompt() == null || configuration.getSystemPrompt().isBlank())) {
            throw new ApiRuntimeException("发布前请选择聊天模型并填写系统提示词");
        }
        validateModel(configuration.getModelConnectionId(), tenantId);
        validateKnowledgeBaseList(configuration, tenantId);
    }

    /** 模型连接属于Agent的租户；平台管理员可跨租户维护，但不能把其他租户凭证绑定给本Agent。 */
    private void validateModel(Long connectionId, Long tenantId) {
        if (connectionId == null) {
            return;
        }
        var connection = connections.requireConnection(connectionId, false);
        if (!Objects.equals(connection.getTenantId(), tenantId)
                || !Objects.equals(connection.getDataStatus(), BasicConstant.DataStatus.ENABLED_CODE)) {
            throw new ApiRuntimeException("模型连接不属于Agent租户或已停用");
        }
        var model = models.requireModel(connection.getModelId());
        if (!ModelType.CHAT.equals(model.getModelType())) {
            throw new ApiRuntimeException("Agent只能选择聊天模型连接");
        }
    }

    /** 逐项进行独立引用授权校验，不拼接知识库展示数据；绑定不替代运行时用户权限检查。 */
    private void validateKnowledgeBaseList(AgentConfiguration configuration, Long tenantId) {
        var ids = configuration.getKnowledgeBaseIds();
        if (ids == null || ids.isEmpty()) {
            return;
        }
        var uniqueIds = new HashSet<>(ids);
        if (uniqueIds.size() != ids.size()) {
            throw new ApiRuntimeException("知识库绑定不能重复");
        }
        for (Long id : ids) {
            var base = knowledgeBases.requireBase(id, false, false);
            if (!Objects.equals(base.getTenantId(), tenantId)
                    || !Objects.equals(base.getDataStatus(), BasicConstant.DataStatus.ENABLED_CODE)) {
                throw new ApiRuntimeException("知识库不属于Agent租户或已停用");
            }
        }
    }
}
