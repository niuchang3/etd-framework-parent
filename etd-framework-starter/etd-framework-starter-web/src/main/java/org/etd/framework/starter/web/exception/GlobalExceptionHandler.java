package org.etd.framework.starter.web.exception;


import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.common.core.constants.RequestCodeConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.stream.Collectors;

/**
 * @author Young
 * @description 统一异常处理
 * @date 2020/6/23
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    /**
     * 缺少必要的 @RequestParam 参数异常处理 状态码：400
     * @param request
     * @param response
     * @param e
     * @return
     */
    
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public ResultModel handle(HttpServletRequest request, HttpServletResponse response, MissingServletRequestParameterException e) {
        log.warn("缺少必要的请求参数: {}", e.getParameterName()); // 这种客户端错误日志级别建议用 warn
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
     * 场景：未传 Body、JSON 格式错误、数据类型无法匹配转换等
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
     * 统一处理API层级异常
     *
     * @param request
     * @param response
     * @param e
     * @return
     */
    
    @ExceptionHandler(value = ApiRuntimeException.class)
    public ResultModel handle(HttpServletRequest request, HttpServletResponse response, ApiRuntimeException e) {
        log.info(e.getMessage(), e);
        return ResultModel.failed(e.getRequestCode(), e, e.getMessage(), request.getRequestURI());
    }

    /**
     * 统一处理运行时异常
     *
     * @param request
     * @param response
     * @param e
     * @return
     */
    
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = RuntimeException.class)
    public ResultModel handle(HttpServletRequest request, HttpServletResponse response, RuntimeException e) {
        log.error(e.getMessage(), e);
        return ResultModel.failed(HttpStatus.INTERNAL_SERVER_ERROR.value(), e, "系统繁忙，请稍后在试", request.getRequestURI());
    }


    /**
     * 统一处理异常
     *
     * @param request
     * @param response
     * @param e
     * @return
     */
    
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = Exception.class)
    public ResultModel handle(HttpServletRequest request, HttpServletResponse response, Exception e) {
        log.error(e.getMessage(), e);
        return ResultModel.failed(HttpStatus.INTERNAL_SERVER_ERROR.value(), e, "系统繁忙，请稍后在试", request.getRequestURI());
    }

}
