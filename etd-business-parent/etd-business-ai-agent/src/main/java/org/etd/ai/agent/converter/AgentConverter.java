package org.etd.ai.agent.converter;

import org.etd.ai.agent.controller.dto.AgentSaveDTO;
import org.etd.ai.agent.controller.vo.AgentConfigurationVO;
import org.etd.ai.agent.controller.vo.AgentKnowledgeOptionVO;
import org.etd.ai.agent.controller.vo.AgentModelOptionVO;
import org.etd.ai.agent.controller.vo.AgentVO;
import org.etd.ai.agent.controller.vo.AgentWriteVO;
import org.etd.ai.agent.entity.AgentEntity;
import org.etd.ai.agent.model.AgentConfiguration;
import org.etd.ai.agent.model.AgentModelOptions;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/** Agent输入、响应及发布快照的编译期转换，禁止映射可信身份及客户端发布字段。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AgentConverter {
    /** 无状态转换实例。 */
    AgentConverter INSTANCE = Mappers.getMapper(AgentConverter.class);

    /** @param input 页面草稿 @return 仅包含可编辑字段的待保存实体 */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "orgId", source = "orgId")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "avatarPath", source = "avatarPath")
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "draftConfig", source = "draftConfig")
    AgentEntity toEntity(AgentSaveDTO input);

    /** @param input 已校验草稿 @param target 加锁记录，只覆盖用户可编辑字段 */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "avatarPath", source = "avatarPath")
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "draftConfig", source = "draftConfig")
    void updateEntity(AgentEntity input, @MappingTarget AgentEntity target);

    /** @param entity 可见记录 @return 不包含运行配置的卡片 */
    AgentVO toVO(AgentEntity entity);
    /** @param entity 已通过维护校验的记录 @return 编辑配置 */
    AgentConfigurationVO toConfigurationVO(AgentEntity entity);
    /** @param entity 写入后的记录 @return 最新ID与版本 */
    AgentWriteVO toWriteVO(AgentEntity entity);
    /** @param entity 已启用的聊天连接 @return 脱敏选择项 */
    AgentModelOptionVO toModelOptionVO(ModelConnectionEntity entity);
    /** @param entity 可读知识库 @return 基本选择项 */
    AgentKnowledgeOptionVO toKnowledgeOptionVO(KnowledgeBaseEntity entity);
    /** @param source 配置源 @return 独立配置副本，避免草稿与发布共用可变列表或参数对象 */
    AgentConfiguration copyConfiguration(AgentConfiguration source);
    /** @param source 模型选项 @return 独立模型选项副本 */
    AgentModelOptions copyModelOptions(AgentModelOptions source);
}
