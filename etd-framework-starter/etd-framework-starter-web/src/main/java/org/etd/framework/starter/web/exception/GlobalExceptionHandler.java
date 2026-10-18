package org.etd.framework.starter.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.constants.RequestCodeConstant;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一异常处理
 *
 * @author Young
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 安全异常交回 Spring Security 过滤器链，避免被通用异常处理转换为 500。 */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void propagateSecurityException(RuntimeException exception) {
        throw exception;
    }

    /**
     * 缺少必要的 @RequestParam 参数异常处理 状态码：400
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public ResultModel handle(HttpServletRequest request, HttpServletResponse response, MissingServletRequestParameterException e) {
        log.warn("缺少必要的请求参数: {}", e.getParameterName());
        return ResultModel.failed(HttpStatus.BAD_REQUEST.value(), e, "缺少必要的请求参数: " + e.getParameterName(), request.getRequestURI());
    }

    /**
     * 拦截请求体校验失败；异常可能包含被拒绝的凭证值，不返回原始异常。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param e 请求体字段校验异常，仅记录异常类型
     * @return 脱敏的 HTTP 400 业务响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResultModel<Void> handle(HttpServletRequest request, HttpServletResponse response, MethodArgumentNotValidException e) {
        return rejectInvalidRequest(request, e);
    }

    /**
     * 拦截路径及查询参数校验失败；自定义校验消息也可能插入原始值，因此统一脱敏。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param e 参数约束异常，不记录原始值或约束消息
     * @return 脱敏的 HTTP 400 业务响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = ConstraintViolationException.class)
    public ResultModel<Void> handle(HttpServletRequest request, HttpServletResponse response, ConstraintViolationException e) {
        return rejectInvalidRequest(request, e);
    }

    /**
     * 拦截请求体解析失败；Jackson 异常可能携带原始报文，开发环境同样不回显堆栈。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param e 请求体解析异常，不读取或记录其消息
     * @return 脱敏的 HTTP 400 业务响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResultModel<Void> handle(HttpServletRequest request, HttpServletResponse response, HttpMessageNotReadableException e) {
        return rejectInvalidRequest(request, e);
    }

    /** 只记录请求路径及异常类型，禁止把原始报文、凭证或被拒绝值带入日志和响应。 */
    private ResultModel<Void> rejectInvalidRequest(HttpServletRequest request, Exception exception) {
        log.warn("请求参数校验或解析失败，路径：{}，异常类型：{}",
                request.getRequestURI(), exception.getClass().getSimpleName());
        return ResultModel.failed(HttpStatus.BAD_REQUEST.value(), null,
                "请求参数不合法，请检查必填项、字段长度和JSON格式", request.getRequestURI());
    }

    /**
     * 数据库唯一约束冲突的通用保护，不把 SQL、字段值或约束名暴露给调用方。
     *
     * @param request 当前请求
     * @param exception 唯一约束异常，不输出其消息或堆栈
     * @return 统一业务失败响应，具体业务规则应提前在业务层校验
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResultModel<Void> handleDuplicateKey(HttpServletRequest request, DuplicateKeyException exception) {
        log.warn("数据库唯一约束冲突，路径：{}", request.getRequestURI());
        return ResultModel.failed(RequestCodeConstant.FAILED, null,
                "数据已存在，请检查唯一性约束", request.getRequestURI());
    }

    /**
     * 数据库引用和字段约束的通用保护，业务专属提示仍由 ApiRuntimeException 表达。
     *
     * @param request 当前请求
     * @param exception 完整性约束异常，不输出 SQL 或字段值
     * @return 不包含数据库内部信息的统一业务失败响应
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResultModel<Void> handleDataIntegrityViolation(HttpServletRequest request, DataIntegrityViolationException exception) {
        log.warn("数据库完整性约束冲突，路径：{}", request.getRequestURI());
        return ResultModel.failed(RequestCodeConstant.FAILED, null,
                "数据不符合约束或仍被引用，请检查后重试", request.getRequestURI());
    }

    /**
     * 拦截 HTTP 请求方法不支持异常 (405)
     */
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public ResultModel handle(HttpServletRequest request, HttpServletResponse response, HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return ResultModel.failed(HttpStatus.METHOD_NOT_ALLOWED.value(), e, "不支持的请求方法: " + e.getMethod(), request.getRequestURI());
    }

    /**
     * 统一处理 API 业务运行时异常
     */
    @ExceptionHandler(value = ApiRuntimeException.class)
    public ResultModel handle(HttpServletRequest request, HttpServletResponse response, ApiRuntimeException e) {
        log.info(e.getMessage(), e);
        return ResultModel.failed(e.getRequestCode(), e, e.getMessage(), request.getRequestURI());
    }

    /**
     * 统一处理运行时异常
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = RuntimeException.class)
    public ResultModel handle(HttpServletRequest request, HttpServletResponse response, RuntimeException e) {
        log.error(e.getMessage(), e);
        return ResultModel.failed(HttpStatus.INTERNAL_SERVER_ERROR.value(), e, "系统繁忙，请稍后再试", request.getRequestURI());
    }

    /**
     * 统一处理系统未捕获异常
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = Exception.class)
    public ResultModel handle(HttpServletRequest request, HttpServletResponse response, Exception e) {
        log.error(e.getMessage(), e);
        return ResultModel.failed(HttpStatus.INTERNAL_SERVER_ERROR.value(), e, "系统繁忙，请稍后再试", request.getRequestURI());
    }
}
