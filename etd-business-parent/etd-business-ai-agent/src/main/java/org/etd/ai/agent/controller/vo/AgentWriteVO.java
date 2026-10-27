package org.etd.ai.agent.controller.vo;

import lombok.Data;

/** 创建、修改、发布和启停后返回最新版本，供页面继续编辑。 */
@Data
public class AgentWriteVO {
    /** Agent ID。 */
    private Long id;
    /** 本次写入后的乐观锁版本。 */
    private Long version;
}
