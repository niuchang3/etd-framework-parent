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
    SUCCESS(2000, "接口请求成功。"),
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

    /**
     * 获取 Code 属性值
     *
     * @return 处理结果
     */
    @Override
    public Integer getCode() {
        return this.code;
    }

    /**
     * 获取 Name 属性值
     *
     * @return 处理结果
     */
    @Override
    public String getName() {
        return this.name();
    }

    /**
     * 获取 Description 属性值
     *
     * @return 处理结果
     */
    @Override
    public String getDescription() {
        return this.description;
    }

}
