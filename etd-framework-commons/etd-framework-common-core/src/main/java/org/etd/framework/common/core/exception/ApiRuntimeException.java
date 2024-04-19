package org.etd.framework.common.core.exception;

import lombok.Data;
import org.etd.framework.common.core.constants.RequestCodeConstant;

/**
 * @author Young
 * @description
 * @date 2020/11/12
 */
@Data
public class ApiRuntimeException extends RuntimeException {

    private RequestCodeConstant requestCode;

    /**
     * 根据RequestCode构建一个API运行时异常
     *
     * @param requestCode
     */
    public ApiRuntimeException(RequestCodeConstant requestCode) {
        super(requestCode.getDescription());
        this.requestCode = requestCode;
    }

    /**
     * 根据参数构建一个API运行时异常
     *
     * @param requestCode 请求Code
     * @param message     异常Message信息
     */
    public ApiRuntimeException(RequestCodeConstant requestCode, String message) {
        super(message);
        this.requestCode = requestCode;
    }


    public ApiRuntimeException(String message) {
        super(message);
        this.requestCode = RequestCodeConstant.FAILED;
    }

}
