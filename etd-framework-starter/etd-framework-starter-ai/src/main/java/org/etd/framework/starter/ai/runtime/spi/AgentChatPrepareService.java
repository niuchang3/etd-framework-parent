package org.etd.framework.starter.ai.runtime.spi;

import org.etd.framework.starter.ai.runtime.model.AgentChatCommand;
import org.etd.framework.starter.ai.runtime.model.AgentChatPreparation;

/** 业务授权与短事务准备；框架获得并发名额后，跨 Bean 调用业务实现。 */
public interface AgentChatPrepareService {
    /**
     * 校验个人会话及 Agent/模型使用资格，按既定锁顺序原子创建 Run 与消息。
     * 返回已批准的资源绑定，不能替代执行阶段独立的知识与工具访问授权。
     * 实现必须通过 Spring 代理开启短事务，不能包含远程调用或组装执行对象。
     * 同幂等标识不得创建第二轮；异常回滚后框架释放名额，不进入异步执行。
     * @param command 入口数据，用户与租户只能从可信请求上下文读取
     * @return 提交事务后独立的本轮授权配置数据
     * @throws RuntimeException 无权限、重复请求、会话占用或持久化失败
     */
    AgentChatPreparation prepare(AgentChatCommand command);
}
