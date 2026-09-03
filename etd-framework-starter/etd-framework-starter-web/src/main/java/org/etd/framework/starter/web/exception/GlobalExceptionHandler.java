package org.etd.framework.starter.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

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
     * 拦截 @Valid / @Validated Body 参数校验失败异常 (400)
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResultModel handle(HttpServletRequest request, HttpServletResponse response, MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String message = bindingResult.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("请求 Body 参数校验失败, 路径: {}, 错误信息: {}", request.getRequestURI(), message);
        return ResultModel.failed(HttpStatus.BAD_REQUEST.value(), e, message, request.getRequestURI());
    }

    /**
     * 拦截 @Validated 单个路径/查询参数校验失败异常 (400)
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = ConstraintViolationException.class)
    public ResultModel handle(HttpServletRequest request, HttpServletResponse response, ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("URL/Query 参数校验失败, 路径: {}, 错误信息: {}", request.getRequestURI(), message);
        return ResultModel.failed(HttpStatus.BAD_REQUEST.value(), e, message, request.getRequestURI());
    }

    /**
     * 拦截请求 Body 解析失败异常 (400)
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResultModel handle(HttpServletRequest request, HttpServletResponse response, HttpMessageNotReadableException e) {
        log.warn("请求 Body 格式错误或缺失, 路径: {}, 异常信息: {}", request.getRequestURI(), e.getMessage());
        String errorMsg = "请求 Body 格式错误或缺失，请检查 JSON 格式与数据类型";

        if (e.getMessage() != null && e.getMessage().contains("Required request body is missing")) {
            errorMsg = "缺少必要的请求 Body 报文";
        }
        return ResultModel.failed(HttpStatus.BAD_REQUEST.value(), e, errorMsg, request.getRequestURI());
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
