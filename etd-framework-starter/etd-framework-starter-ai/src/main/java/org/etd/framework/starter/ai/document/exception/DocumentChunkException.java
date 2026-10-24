package org.etd.framework.starter.ai.document.exception;

/** 可向业务层安全展示的分片规则异常；仅使用固定原因，不携带原文、正则或远端响应。 */
public final class DocumentChunkException extends IllegalArgumentException {
    /** 固定原因用于生成用户提示及安全诊断代码。 */
    private final Reason reason;

    /** @param reason 已定义的分片失败原因，不接受动态异常消息 */
    public DocumentChunkException(Reason reason) {
        super(reason.getMessage());
        this.reason = reason;
    }

    /** @return 不包含正文或远端错误的固定原因 */
    public Reason getReason() {
        return reason;
    }

    /** 分片规则失败原因，code用于日志诊断，message可直接展示给用户。 */
    public enum Reason {
        /** 结构保护与Token上限冲突，须调整上限而非破坏原子结构。 */
        PROTECTED_BLOCK_TOO_LARGE("protected_block_too_large", "表格或代码块超过可用正文Token预算，请提高上限或减小重叠后重试"),
        /** 原生结果缺少正文时不能仅用借入的上下文生成一片伪正文。 */
        OVERLAP_CORE_MISSING("overlap_core_missing", "解析引擎未提供有效的分片正文，无法追加重叠"),
        /** 原生引擎与本地分词器计数不同，核心正文已超过最终上限时不能删除正文强行满足预算。 */
        OVERLAP_CORE_TOO_LARGE("overlap_core_too_large", "分片正文按cl100k_base计数超过最终Token上限，请提高上限或减小重叠后重试"),
        /** 表达式匹配时间或字符读取预算耗尽。 */
        REGEX_BUDGET_EXCEEDED("regex_budget_exceeded", "正则匹配超过执行预算，请简化表达式"),
        /** 正则递归层数超过运行时可处理范围。 */
        REGEX_TOO_DEEP("regex_too_deep", "正则递归过深，请简化表达式");

        /** 稳定诊断代码，不使用枚举序号作为协议值。 */
        private final String code;
        /** 固定中文提示，禁止拼接原文、正则或第三方错误。 */
        private final String message;

        Reason(String code, String message) {
            this.code = code;
            this.message = message;
        }

        /** @return 固定诊断代码 */
        public String getCode() {
            return code;
        }

        /** @return 可安全展示的中文原因 */
        public String getMessage() {
            return message;
        }
    }
}
