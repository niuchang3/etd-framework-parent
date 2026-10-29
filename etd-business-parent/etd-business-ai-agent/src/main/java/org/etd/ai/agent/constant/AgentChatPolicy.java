package org.etd.ai.agent.constant;

/** 聊天协议、数据库状态及默认边界，避免跨层使用分散字面量。 */
public final class AgentChatPolicy {
    /** 输出插槽关联会话的元数据键。 */
    public static final String SESSION_ID = "sessionId";
    /** 输出插槽关联助手消息的元数据键。 */
    public static final String MESSAGE_ID = "messageId";
    /** 业务引用在通用知识片段中的元数据键。 */
    public static final String CITATION = "citation";
    /** 创建及展示接口共同允许的会话类型。 */
    public static final String CONVERSATION_TYPE_PATTERN = "chat|debug";
    /** 正式会话，使用发布配置。 */
    public static final String CHAT = "chat";
    /** 调试会话，使用草稿并要求Agent维护权限。 */
    public static final String DEBUG = "debug";
    /** 活跃执行。 */
    public static final String RUNNING = "running";
    /** 已请求取消，仍占用会话执行位置。 */
    public static final String CANCEL_REQUESTED = "cancel_requested";
    /** 成功终态。 */
    public static final String SUCCEEDED = "succeeded";
    /** 失败终态，同时作为失败消息状态。 */
    public static final String FAILED = "failed";
    /** 取消终态。 */
    public static final String CANCELLED = "cancelled";
    /** 正在生成的助手消息。 */
    public static final String GENERATING = "generating";
    /** 完成的消息。 */
    public static final String COMPLETED = "completed";
    /** 保留部分内容的中断消息。 */
    public static final String INTERRUPTED = "interrupted";
    /** 用户角色。 */
    public static final String USER = "user";
    /** 助手角色。 */
    public static final String ASSISTANT = "assistant";
    /** SSE执行开始事件。 */
    public static final String START = "start";
    /** SSE引用事件。 */
    public static final String REFERENCES = "references";
    /** SSE文本增量事件。 */
    public static final String DELTA = "delta";
    /** SSE完成事件。 */
    public static final String DONE = "done";
    /** SSE错误事件。 */
    public static final String ERROR = "error";
    /** 默认历史消息条数上限，控制在合理轮数内降低首字延迟。 */
    public static final int DEFAULT_HISTORY_LIMIT = 8;
    /** 用户问题最大字符数，与知识检索入口保持一致。 */
    public static final int MAX_QUESTION_CHARACTERS = 2000;
    /** 租约到期错误码；仅标记失败，不自动重放模型调用。 */
    public static final String LEASE_EXPIRED = "AGENT_LEASE_EXPIRED";

    private AgentChatPolicy() {
    }
}
