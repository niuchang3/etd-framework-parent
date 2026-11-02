package org.etd.framework.starter.ai.runtime;

import java.util.concurrent.TimeUnit;
import org.etd.framework.starter.ai.agent.AgentExecutor;
import org.etd.framework.starter.ai.persistence.spi.AgentRunStore;
import org.etd.framework.starter.ai.persistence.spi.AgentRunLeaseStore;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.framework.starter.ai.runtime.model.AgentChatCommand;
import org.etd.framework.starter.ai.runtime.spi.AgentChatPrepareService;
import org.etd.framework.starter.ai.adapter.AgentRunEventListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.Assert;
import reactor.core.Disposable;

/** 聊天统一入口：协调业务短事务与框架执行，不要求业务连接模型、检索或线程回调。 */
public class AgentChatService {
    private static final long OUTPUT_TIMEOUT_GRACE_SECONDS = 10;
    private final AgentTaskRunner taskRunner;
    private final AgentExecutor executor;
    private final AgentRuntimeProperties properties;
    private final ObjectProvider<AgentChatPrepareService> preparation;
    private final ObjectProvider<AgentRunStore> records;
    private final ObjectProvider<AgentRunLeaseStore> leases;

    /** 业务扩展按需解析，避免自动配置与业务 Bean 之间形成初始化循环。 */
    public AgentChatService(AgentTaskRunner taskRunner, AgentExecutor executor, AgentRuntimeProperties properties,
            ObjectProvider<AgentChatPrepareService> preparation, ObjectProvider<AgentRunStore> records,
            ObjectProvider<AgentRunLeaseStore> leases) {
        this.taskRunner = taskRunner;
        this.executor = executor;
        this.properties = properties;
        this.preparation = preparation;
        this.records = records;
        this.leases = leases;
    }

    /** @return 输出通道超时，包含 Run 期限后的终态持久化余量 */
    public long getOutputTimeoutMillis() {
        return TimeUnit.SECONDS.toMillis(properties.getTimeoutSeconds() + OUTPUT_TIMEOUT_GRACE_SECONDS);
    }

    /**
     * 准入通过后调用业务短事务，后台工作由框架独立管理；失败释放容量并保存失败终态。
     * @param command 本轮会话和问题数据，不含客户端提供的身份
     * @param output 协议适配器，每次请求独立创建
     * @return 官方 Reactor 取消句柄；Run 标识通过开始事件返回，不向业务暴露内部执行控制
     */
    public Disposable start(AgentChatCommand command, AgentRunEventListener output) {
        Assert.notNull(command, "聊天请求不能为空");
        Assert.notNull(output, "事件监听器不能为空");
        AgentChatPrepareService prepare = preparation.getObject();
        AgentRunStore store = records.getObject();
        AgentRunLeaseStore lease = leases.getObject();
        return taskRunner.start(command, output, prepare, store, lease, executor);
    }

}
