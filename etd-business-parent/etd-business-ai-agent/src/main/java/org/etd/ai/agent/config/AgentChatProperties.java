package org.etd.ai.agent.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 业务运行记录的数据库租约策略；通用执行预算由框架etd.ai.runtime配置。 */
@Data
@Validated
@ConfigurationProperties("etd.ai.chat")
public class AgentChatProperties {
    /** 数据库运行租约秒数，每秒通过生命周期插槽续租，默认30秒。 */
    @Min(5) @Max(300)
    private int leaseSeconds = 30;
}
