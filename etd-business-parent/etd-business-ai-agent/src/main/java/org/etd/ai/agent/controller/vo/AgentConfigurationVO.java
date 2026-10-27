package org.etd.ai.agent.controller.vo;

import java.time.Instant;
import lombok.Data;
import org.etd.ai.agent.model.AgentConfiguration;

/** 仅向具有维护资格的用户返回的Agent编辑配置。 */
@Data
public class AgentConfigurationVO {
    /** Agent ID。 */
    private Long id;
    /** 当前乐观锁版本。 */
    private Long version;
    /** 当前草稿配置。 */
    private AgentConfiguration draftConfig;
    /** 当前发布配置，未发布时为空。 */
    private AgentConfiguration publishedConfig;
    /** 最近发布时间点。 */
    private Instant publishedTime;
    /** 最近发布用户ID。 */
    private Long publishedBy;
}
