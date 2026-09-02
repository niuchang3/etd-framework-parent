package org.etd.upms.config.constant;

import org.etd.framework.common.core.exception.ApiRuntimeException;

import java.util.Arrays;

/**
 * 系统参数值类型，与服务端字典 system_config_value_type 的字典值保持一致。
 */
public enum SystemConfigValueType {

    STRING("string"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    JSON("json");

    private final String code;

    SystemConfigValueType(String code) {
        this.code = code;
    }

    /**
     * 获取 Code 属性值
     *
     * @return 处理结果
     */
    public String getCode() {
        return code;
    }

    /**
     * 从 Code
     *
     * @param code 参数 code
     * @return 处理结果
     */
    public static SystemConfigValueType fromCode(String code) {
        return Arrays.stream(values())
                .filter(valueType -> valueType.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new ApiRuntimeException("不支持的参数值类型。"));
    }
}
