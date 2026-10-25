package org.etd.framework.starter.ai.model.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/** 模型实例缓存及共享HTTP传输配置，不包含全局模型或全局凭证。 */
@Data
@ConfigurationProperties("etd.ai.model.clients")
public class ModelClientProperties {
    /** 每个应用节点的Map缓存条目上限，包含同一连接的不同配置版本。 */
    private int maximumSize = 128;
    /** 无新获取请求后保留10分钟；下次访问时清理，淘汰不关闭在途请求持有的原生模型。 */
    private Duration expireAfterAccess = Duration.ofMinutes(10);
    /** 与模型服务器建立连接的最长等待时间，默认10秒。 */
    private Duration connectTimeout = Duration.ofSeconds(10);
    /** 等待模型HTTP响应的超时时间，默认60秒；业务仍需限制完整执行时长。 */
    private Duration readTimeout = Duration.ofSeconds(60);

    /** 创建组件前校验配置，拒绝零值、负值及无法转换为纳秒的过大时长。 */
    public void validate() {
        Assert.isTrue(maximumSize > 0, "模型客户端缓存容量必须大于0");
        requirePositive(expireAfterAccess);
        requirePositive(connectTimeout);
        requirePositive(readTimeout);
    }

    private void requirePositive(Duration value) {
        Assert.notNull(value, "模型客户端时长配置不能为空");
        Assert.isTrue(!value.isNegative() && !value.isZero(), "模型客户端时长配置必须大于0");
        value.toNanos();
    }
}
