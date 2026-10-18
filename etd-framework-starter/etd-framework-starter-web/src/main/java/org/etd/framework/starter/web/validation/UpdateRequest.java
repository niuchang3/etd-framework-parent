package org.etd.framework.starter.web.validation;

import jakarta.validation.groups.Default;

/** 更新请求校验分组，同时执行默认字段约束；用于新增可省略、更新必填的版本号等属性。 */
public interface UpdateRequest extends Default {
}
