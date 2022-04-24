package org.etd.framework.common.core.constants;

/**
 * @author Young
 * @description
 * @date 2020/11/12
 */

public enum RequestCodeConstant implements RequestCodeConverter {
    /**
     * 调用成功
     */
    SUCCESS(200, "接口请求成功。"),


    NO_PERMISSION(4000, "请登录后在进行操作。"),

    /**
     * 请求资源权限不足。
     */
    NO_URL_PERMISSION(403, "请求资源权限不足。"),


    /**
     * 服务器代码报错
     */
    INTERNAL_SERVER_ERROR(500, "服务器代码报错。"),
    /**
     * 业务状态码：5000
     */
    FAILED(5001, "操作失败"),

    /**
     * 数据校验异常
     */
    VALIDATE_ERROR(5002, "数据校验异常");

    /**
     * 错误码
     */
    private Integer code;
    /**
     * 错误码描述
     */
    private String description;


    RequestCodeConstant(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public Integer getCode() {
        return this.code;
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getDescription() {
        return this.description;
    }

}
