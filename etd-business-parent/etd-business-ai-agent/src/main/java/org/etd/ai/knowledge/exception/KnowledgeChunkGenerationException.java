package org.etd.ai.knowledge.exception;

import java.math.BigDecimal;
import java.time.Duration;

/** 分片生成的可展示失败；动态信息仅由时限和页数构成，禁止携带文档正文或服务商原始错误。 */
public final class KnowledgeChunkGenerationException extends RuntimeException {
    /** 用于日志分类的稳定失败原因。 */
    private final Reason reason;

    /** @param reason 固定失败原因，不允许外部传入任意消息 */
    public KnowledgeChunkGenerationException(Reason reason) {
        this(reason, reason.getMessage());
    }

    private KnowledgeChunkGenerationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    /** @return 不包含原文的稳定诊断分类 */
    public Reason getReason() {
        return reason;
    }

    /** @param timeout 用户配置的单文档解析时限 @param completed 已完成页数，可为空
     * @param total 需处理页数，可为空 @return 带安全时限及可用进度的解析超时 */
    public static KnowledgeChunkGenerationException parseTimeout(Duration timeout, Integer completed, Integer total) {
        String message = "文档解析超时：已达到配置的" + formatSeconds(timeout) + "秒处理时限";
        if (completed != null && total != null && total > 0 && completed >= 0 && completed <= total) {
            message += "，已完成" + completed + "/" + total + "页，" + (total - completed) + "页失败或未完成";
        }
        message += "；请增大文档处理超时或缩小解析页码范围后重试";
        return new KnowledgeChunkGenerationException(Reason.PARSE_TIMEOUT, message);
    }

    /** @param budget 包含提交、排队和取回结果预留时间的总等待预算 @return 独立于文档解析超时的等待超时 */
    public static KnowledgeChunkGenerationException waitTimeout(Duration budget) {
        String message = "等待文档解析任务完成超时：已超过" + formatSeconds(budget)
                + "秒等待预算（含提交与获取结果的预留时间）；请检查服务排队与负载后重试";
        return new KnowledgeChunkGenerationException(Reason.WAIT_TIMEOUT, message);
    }

    /** 避免整秒被输出为90.0，同时保留用户配置的亚秒精度。 */
    private static String formatSeconds(Duration duration) {
        var seconds = BigDecimal.valueOf(duration.getSeconds());
        var fraction = BigDecimal.valueOf(duration.getNano(), 9);
        return seconds.add(fraction).stripTrailingZeros().toPlainString();
    }

    /** 仅供知识库生成流程使用的固定分类，code用于日志，message可展示给用户。 */
    public enum Reason {
        /** 引擎已经明确报告文档处理超时。 */
        PARSE_TIMEOUT("parse_timeout", "文档解析超时，请增大处理时限或缩小页码范围"),
        /** 业务轮询等待预算耗尽，不能冒充引擎已经报告解析超时。 */
        WAIT_TIMEOUT("wait_timeout", "等待文档解析任务完成超时，请检查服务排队与负载"),
        /** 文档仅部分成功，原因不在可识别的安全诊断范围内。 */
        PARTIAL_PARSE("partial_parse", "文档仅部分解析成功，未生成有效分片，请检查文件及解析参数"),
        /** 引擎报告文档失败。 */
        PARSE_FAILED("parse_failed", "文档解析失败，请检查文件格式、文件内容及解析参数"),
        /** 引擎主动跳过文档。 */
        DOCUMENT_SKIPPED("document_skipped", "解析服务跳过了该文档，请检查文件格式是否受支持"),
        /** 单文件请求没有返回唯一的文档结果。 */
        INVALID_RESULT_COUNT("invalid_result_count", "解析服务返回的文档结果数量异常，单份资料必须返回一份文档结果"),
        /** 异步任务整体执行失败，尚未取得文档级诊断。 */
        TASK_FAILED("task_failed", "文档解析任务执行失败，请联系管理员检查服务运行状态"),
        /** 解析服务的HTTP连接被中断，不据此向用户断言具体部署故障。 */
        SERVICE_CONNECTION_INTERRUPTED("service_connection_interrupted", "文档解析服务连接中断，请稍后重试；若持续失败请联系管理员检查服务运行状态"),
        /** 调用解析服务时等待HTTP响应超时，区别于文档内部处理时限。 */
        SERVICE_REQUEST_TIMEOUT("service_request_timeout", "文档解析服务请求超时，请稍后重试或联系管理员检查服务负载"),
        /** 未取得可安全展示的细分原因时，不向接口返回SDK或引擎信息。 */
        SERVICE_CALL_FAILED("service_call_failed", "文档解析服务调用失败，请稍后重试或联系管理员"),
        /** 请求线程被中断，不应继续等待或发布结果。 */
        INTERRUPTED("interrupted", "分片生成请求已中断，请重新提交生成请求");

        /** 稳定日志代码，不使用枚举名称或序号作为协议。 */
        private final String code;
        /** 不包含远端原始信息的中文提示。 */
        private final String message;

        Reason(String code, String message) {
            this.code = code;
            this.message = message;
        }

        /** @return 稳定诊断代码 */
        public String getCode() {
            return code;
        }

        /** @return 可安全展示的中文原因 */
        public String getMessage() {
            return message;
        }
    }
}
