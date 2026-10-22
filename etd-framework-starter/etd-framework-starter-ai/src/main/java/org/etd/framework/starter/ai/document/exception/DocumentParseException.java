package org.etd.framework.starter.ai.document.exception;

/** 解析技术异常；消息不包含远端响应正文，原始原因仅供受控诊断，禁止直接返回客户端。 */
public class DocumentParseException extends RuntimeException {
    /** 没有取得远端 HTTP 响应时的状态哨兵值，不是有效 HTTP 状态码。 */
    public static final int NO_HTTP_STATUS = -1;

    /** 远端 HTTP 状态；无 HTTP 响应时为 -1。 */
    private final int httpStatus;

    /**
     * @param message 安全的技术提示
     * @param httpStatus HTTP 状态，无响应时为 -1
     * @param cause 原始异常，可能含敏感内容，不得自动输出到接口响应
     */
    public DocumentParseException(String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    /** @return 远端 HTTP 状态或 -1 */
    public int getHttpStatus() {
        return httpStatus;
    }
}
