package org.etd.framework.starter.ai.document.constant;

/** 任务执行状态；完成不等于文档完整成功。 */
public enum DocumentTaskStatus {
    /** 排队。 */
    PENDING("pending"),
    /** 执行中。 */
    RUNNING("running"),
    /** 执行完成。 */
    COMPLETED("completed"),
    /** 执行失败。 */
    FAILED("failed");

    /** 稳定协议代码，不依赖枚举序号或名称。 */
    private final String code;

    DocumentTaskStatus(String code) {
        this.code = code;
    }

    /** @return 稳定协议代码 */
    public String getCode() {
        return code;
    }
}
