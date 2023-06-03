package org.etd.framework.starter.web.exception;


import lombok.extern.slf4j.Slf4j;
import org.etd.framework.common.core.constants.RequestCodeConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Young
 * @description 统一异常处理
 * @date 2020/6/23
 */

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {


    /**
     * 统一处理API层级异常
     *
     * @param request
     * @param response
     * @param e
     * @return
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(value = ApiRuntimeException.class)
    public ResultModel handle(HttpServletRequest request, HttpServletResponse response, ApiRuntimeException e) {
        log.error(e.getMessage(), e);
        return ResultModel.failed(e.getRequestCode(), e, e.getMessage(), request.getRequestURI());
    }


    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(value = HttpClientErrorException.Unauthorized.class)
    public ResultModel handle(HttpServletRequest request, HttpServletResponse response, HttpClientErrorException.Unauthorized e) {
        log.error(e.getMessage(), e);
        return ResultModel.failed(RequestCodeConstant.NO_PERMISSION, e, e.getMessage(), request.getRequestURI());
    }


    /**
     * 统一处理运行时异常
     *
     * @param request
     * @param response
     * @param e
     * @return
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = RuntimeException.class)
    public ResultModel handle(HttpServletRequest request, HttpServletResponse response, RuntimeException e) {
        log.error(e.getMessage(), e);
        return ResultModel.failed(RequestCodeConstant.INTERNAL_SERVER_ERROR, e, "系统繁忙，请稍后在试", request.getRequestURI());
    }


    /**
     * 统一处理异常
     *
     * @param request
     * @param response
     * @param e
     * @return
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = Exception.class)
    public ResultModel handle(HttpServletRequest request, HttpServletResponse response, Exception e) {
        log.error(e.getMessage(), e);
        return ResultModel.failed(RequestCodeConstant.INTERNAL_SERVER_ERROR, e, "系统繁忙，请稍后在试", request.getRequestURI());
    }

}
