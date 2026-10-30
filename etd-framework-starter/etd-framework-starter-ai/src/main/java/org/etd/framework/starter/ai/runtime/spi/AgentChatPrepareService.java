package org.etd.framework.starter.ai.runtime.spi;

import org.etd.framework.starter.ai.runtime.model.AgentChatPreparation;
import org.etd.framework.starter.ai.runtime.model.AgentChatRequest;

/** 业务实现的聊天准备接口，注册为Spring Bean后由统一聊天入口调用，不需要组装其他接口。 */
public interface AgentChatPrepareService {
    /** 在调用线程、容量检查通过后执行一次，继承当前请求的租户及认证上下文。
     * 实现方应使用短事务完成会话授权、幂等及忙闲校验，读取配置和历史，建立运行及消息记录。
     * 返回前必须提交事务，禁止在该事务内调用模型或外部检索；发生异常应整体回滚并直接抛出。
     * @param request 本轮聊天参数 @return 已授权的本轮数据，执行标识及模型连接信息必须有效
     * @throws RuntimeException 无权限、重复请求、会话忙或业务准备失败，此时框架不启动模型任务
     */
    AgentChatPreparation prepareChat(AgentChatRequest request);
}
