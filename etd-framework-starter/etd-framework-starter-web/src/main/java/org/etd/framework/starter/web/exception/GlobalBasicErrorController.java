package org.etd.framework.starter.web.exception;

import cn.hutool.core.exceptions.ExceptionUtil;
import org.etd.framework.common.core.constants.RequestCodeConstant;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;


@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class GlobalBasicErrorController implements ErrorController {

    private static final String ERROR_INTERNAL_ATTRIBUTE = DefaultErrorAttributes.class.getName() + ".ERROR";

    @ResponseBody
    @RequestMapping
    public ResultModel error(HttpServletRequest request) {
        WebRequest webRequest = new ServletWebRequest(request);
        Throwable error = getError(webRequest);
        String path = getAttribute(webRequest, RequestDispatcher.ERROR_REQUEST_URI);
        return ResultModel.failed(RequestCodeConstant.INTERNAL_SERVER_ERROR, error, getMessage(webRequest, error), path);
    }

    @ResponseBody
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResultModel mediaTypeNotAcceptable(HttpServletRequest request) {
        WebRequest webRequest = new ServletWebRequest(request);
        Throwable error = getError(webRequest);
        String path = getAttribute(webRequest, RequestDispatcher.ERROR_REQUEST_URI);
        return ResultModel.failed(RequestCodeConstant.INTERNAL_SERVER_ERROR, error, getMessage(webRequest, error), path);
    }

    protected String getMessage(WebRequest webRequest, Throwable error) {
        Object message = getAttribute(webRequest, RequestDispatcher.ERROR_MESSAGE);
        if (!ObjectUtils.isEmpty(message)) {
            return message.toString();
        }
        if (error != null && StringUtils.hasLength(error.getMessage())) {
            return error.getMessage();
        }
        return "No message available";
    }

    public Throwable getError(WebRequest webRequest) {
        Throwable exception = getAttribute(webRequest, ERROR_INTERNAL_ATTRIBUTE);
        if (exception == null) {
            exception = getAttribute(webRequest, RequestDispatcher.ERROR_EXCEPTION);
        }
        // store the exception in a well-known attribute to make it available to metrics
        // instrumentation.
        webRequest.setAttribute(ErrorAttributes.ERROR_ATTRIBUTE, exception, WebRequest.SCOPE_REQUEST);
        return exception;
    }

    private <T> T getAttribute(RequestAttributes requestAttributes, String name) {
        return (T) requestAttributes.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
    }

    private String getStackTraceError(Throwable trace) {
        return ExceptionUtil.stacktraceToString(trace);
    }
}
