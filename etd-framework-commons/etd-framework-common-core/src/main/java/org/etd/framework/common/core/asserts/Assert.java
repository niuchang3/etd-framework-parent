package org.etd.framework.common.core.asserts;


import org.etd.framework.common.core.constants.RequestCodeConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.util.ObjectUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Iterator;
import java.util.Set;

/**
 * 断言工具类
 *
 * @author Administrator
 */
public class Assert {

    /**
     * 校验字段
     *
     * @param object
     * @param <T>
     */
    public static <T> void validate(T object) {
        // 获得验证器
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        // 执行验证
        Set<ConstraintViolation<T>> constraintViolations = validator.validate(object);
        // 如果有验证信息，则将第一个取出来包装成异常返回
        if (!constraintViolations.isEmpty()) {
            for (Iterator<ConstraintViolation<T>> iterator = constraintViolations.iterator(); iterator.hasNext(); ) {
                ConstraintViolation<T> constraintViolation = (ConstraintViolation<T>) iterator.next();
                throw new ApiRuntimeException(RequestCodeConstant.VALIDATE_ERROR, constraintViolation.getMessage());
            }
        }
    }

    /**
     * 校验对象是否为null
     *
     * @param object
     * @param message
     */
    public final static void isNotNull(Object object, String message) {
        if (!ObjectUtils.isEmpty(object)) {
            return;
        }
        throw new ApiRuntimeException(RequestCodeConstant.VALIDATE_ERROR, message);
    }

    /**
     * 校验对象是否为null
     *
     * @param object
     * @param message
     */
    public final static void isNull(Object object, String message) {
        if (ObjectUtils.isEmpty(object)) {
            return;
        }
        throw new ApiRuntimeException(RequestCodeConstant.VALIDATE_ERROR, message);
    }

    /**
     * 抛出校验异常
     *
     * @param message
     */
    public static void throwValidationException(String message) {
        throw new ApiRuntimeException(RequestCodeConstant.VALIDATE_ERROR, message);
    }
}
