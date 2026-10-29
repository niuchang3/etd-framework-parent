package org.etd.ai.agent.converter;

import org.etd.ai.agent.controller.vo.AgentChatSessionVO;
import org.etd.ai.agent.controller.vo.AgentChatProfileVO;
import org.etd.ai.agent.entity.AgentEntity;
import org.etd.ai.agent.model.AgentConfiguration;
import org.etd.ai.agent.controller.vo.AgentChatMessageVO;
import org.etd.ai.agent.controller.vo.AgentChatRunVO;
import org.etd.ai.agent.entity.AgentConversationEntity;
import org.etd.ai.agent.entity.AgentMessageEntity;
import org.etd.ai.agent.entity.AgentRunEntity;
import org.etd.ai.agent.model.AgentCitation;
import java.util.List;
import java.util.ArrayList;
import org.etd.ai.agent.constant.AgentChatPolicy;
import org.etd.framework.starter.ai.runtime.model.KnowledgeFragment;
import org.etd.ai.knowledge.model.KnowledgeRecallHit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/** 聊天持久化模型与接口响应的编译期映射。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AgentChatConverter {
    /** 无状态转换实例。 */
    AgentChatConverter INSTANCE = Mappers.getMapper(AgentChatConverter.class);
    /** @param references 框架实际使用的业务来源 @return 按上下文顺序编号的引用列表 */
    default List<AgentCitation> toCitationList(List<KnowledgeFragment> references) {
        List<AgentCitation> result = new ArrayList<>();
        for (KnowledgeFragment reference : references) {
            AgentCitation citation = (AgentCitation) reference.metadata().get(AgentChatPolicy.CITATION);
            citation.setNumber(result.size() + 1);
            result.add(citation);
        }
        return List.copyOf(result);
    }

    /** @param agent 已授权Agent @param configuration 正式或调试展示配置 @return 不含内部指令的聊天资料 */
    @Mapping(target = "agentId", source = "agent.id")
    @Mapping(target = "name", source = "agent.name")
    @Mapping(target = "description", source = "agent.description")
    @Mapping(target = "avatarPath", source = "agent.avatarPath")
    @Mapping(target = "citationEnabled", expression = "java(!Boolean.FALSE.equals(configuration.getCitationEnabled()))")
    AgentChatProfileVO toProfileVO(AgentEntity agent, AgentConfiguration configuration);

    /** @param entity 会话 @return 对外会话 */
    AgentChatSessionVO toSessionVO(AgentConversationEntity entity);
    /** @param entities 会话列表 @return 保持原顺序的接口会话列表 */
    List<AgentChatSessionVO> toSessionVOList(List<AgentConversationEntity> entities);
    /** @param entity 消息 @return 对外消息 */
    AgentChatMessageVO toMessageVO(AgentMessageEntity entity);
    /** @param entities 消息列表 @return 保持原顺序的接口消息列表 */
    List<AgentChatMessageVO> toMessageVOList(List<AgentMessageEntity> entities);
    /** @param entity 执行记录 @return 对外状态 */
    AgentChatRunVO toRunVO(AgentRunEntity entity);
    /** @param hit 已授权候选 @param baseId 知识库ID @return 不含正文的引用 */
    @Mapping(target = "number", ignore = true)
    @Mapping(target = "knowledgeBaseId", source = "baseId")
    AgentCitation toCitation(KnowledgeRecallHit hit, Long baseId);
}
