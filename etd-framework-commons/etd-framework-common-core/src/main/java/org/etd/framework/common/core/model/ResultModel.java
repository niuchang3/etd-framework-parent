package org.etd.framework.common.core.model;


import cn.hutool.core.exceptions.ExceptionUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.etd.framework.common.core.constants.RequestCodeConstant;
import org.etd.framework.common.core.constants.RequestCodeConverter;
import org.etd.framework.common.core.spring.SpringContextHelper;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;

/**
 * 数据统一返回模型
 *
 * @author Young
 * @description
 * @date 2020/6/23
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ResultModel<T> implements Serializable {

    /** 开发环境 Profile，用于控制异常堆栈是否写入响应。 */
    private static final String DEVELOPMENT_PROFILE = "dev";

    /**
     * 操作码
     */
    private Integer code;
    /**
     * 提供给开发人员看的信息
     */
    private String devMessage;
    /**
     * 需要提示用户的信息
     */
    private String message;
    /**
     * 请求返回数据
     */
    private T data;
    /**
     * 请求URL
     */
    private String url;


    /**
     * 操作成功，返回数据结果集
     *
     * @param data
     * @param <T>
     * @return
     */
    /**
     * success
     *
     * @param data 参数 data
     * @return 处理结果
     */
    public static <T> ResultModel<T> success(T data) {

        return new ResultModel(RequestCodeConstant.SUCCESS.getCode(), RequestCodeConstant.SUCCESS.getDescription(), "", data, "");
    }

    /**
     * 操作失败
     *
     * @param <T>
     * @return
     */
    /**
     * failed
     *
     * @param requestCode 参数 requestCode
     * @param throwable 参数 throwable
     * @param message 参数 message
     * @param url 参数 url
     * @return 处理结果
     */
    public static <T> ResultModel<T> failed(RequestCodeConverter requestCode, Throwable throwable, String message, String url) {
        return new ResultModel(requestCode.getCode(), getDevMessage(throwable), message, "", url);
    }

    /**
     * 操作失败
     *
     * @param <T>
     * @return
     */
    /**
     * failed
     *
     * @param requestCode 参数 requestCode
     * @param throwable 参数 throwable
     * @param message 参数 message
     * @param url 参数 url
     * @return 处理结果
     */
    public static <T> ResultModel<T> failed(Integer requestCode, Throwable throwable, String message, String url) {
        return new ResultModel(requestCode, getDevMessage(throwable), message, "", url);
    }
    /**
     * 开发环境会获取栈信息，方便开发人员调试异常
     *
     * @param throwable
     * @return
     */
    private static String getDevMessage(Throwable throwable) {
        if (ObjectUtils.isEmpty(throwable)) {
            return null;
        }
        ApplicationContext applicationContext = SpringContextHelper.getApplicationContext();
        if (applicationContext == null) {
            return null;
        }
        Environment environment = applicationContext.getEnvironment();
        if (environment.acceptsProfiles(Profiles.of(DEVELOPMENT_PROFILE))) {
            return ExceptionUtil.stacktraceToString(throwable);
        }
        return null;
    }
}
