package org.etd.framework.common.utils.exception.code;

import lombok.Getter;

/**
 * @author Young
 * @description
 * @date 2020/11/12
 */

public enum RequestCode {
	/**
	 * 调用成功
	 */
	SUCCESS(2000, "接口请求成功。"),

	/**
	 * 数据校验异常
	 * 业务状态码：4000 ，[认证权限等操作]，如何权限相关的操作码从4000开始向上定义
	 */
	NO_PERMISSION(4000, "权限不足。"),

	/**
	 * 服务器代码报错
	 */
	INTERNAL_SERVER_ERROR(500, "服务器代码报错。"),
	/**
	 * 业务状态码：5000
	 */
	FAILED(5000, "操作失败"),

	/**
	 * 数据校验异常
	 */
	VALIDATE_ERROR(5001, "数据校验异常");

	/**
	 * 错误码
	 */
	@Getter
	private Integer code;
	/**
	 * 错误码描述
	 */
	@Getter
	private String description;


	RequestCode(Integer code, String description) {
		this.code = code;
		this.description = description;
	}
}
