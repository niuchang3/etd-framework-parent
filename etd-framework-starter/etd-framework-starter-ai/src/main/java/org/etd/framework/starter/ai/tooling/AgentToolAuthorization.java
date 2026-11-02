package org.etd.framework.starter.ai.tooling;

/** 本地或 MCP 工具的业务执行资格；上下文传递不代替授权判断。 */
public interface AgentToolAuthorization {
    /**
     * 发现阶段与每次实际执行前调用；身份仅从框架恢复的可信上下文读取。
     * 不开启长事务，不接收模型传入的身份；并发与重复调用必须无副作用。
     * @param toolName 官方 ToolDefinition 中的工具名称
     * @throws RuntimeException 当前用户无执行资格时明确拒绝，框架终止 Run
     */
    void requireAuthorized(String toolName);
}
