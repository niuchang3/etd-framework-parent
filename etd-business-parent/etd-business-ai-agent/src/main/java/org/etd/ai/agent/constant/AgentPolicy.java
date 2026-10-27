package org.etd.ai.agent.constant;

/** Agent配置边界和数据权限使用的固定约定。 */
public final class AgentPolicy {
    /** 数据权限公开读取列，维护操作仍使用严格角色范围。 */
    public static final String PUBLIC_COLUMN = "is_public";
    /** 单个Agent最多绑定的知识库数量。 */
    public static final int MAX_KNOWLEDGE_BASES = 100;
    /** 系统提示词最大字符数。 */
    public static final int MAX_PROMPT_LENGTH = 32000;
    /** 最多纳入上下文的历史消息条数，仍需受模型Token预算约束。 */
    public static final int MAX_HISTORY_MESSAGES = 100;
    /** 最多展示的推荐问题数量。 */
    public static final int MAX_SUGGESTED_QUESTIONS = 10;
    /** 单条推荐问题最大字符数。 */
    public static final int MAX_QUESTION_LENGTH = 500;
    /** 开场白最大字符数。 */
    public static final int MAX_OPENING_LENGTH = 2000;

    private AgentPolicy() {
    }
}
