package org.etd.framework.common.core.constants;

import lombok.Getter;

/**
 * @author Young
 * @description
 * @date 2020/11/12
 */

public enum RequestCodeConstant {
	/**
	 * 调用成功
	 */
	SUCCESS(2000, "接口请求成功。"),

	/**
	 * 数据校验异常
	 * 业务状态码：4000 ，[认证权限等操作]，如何权限相关的操作码从4000开始向上定义
	 */
	NO_PERMISSION(401, "请登录后在进行操作。"),

	/**
	 * 请求资源权限不足。
	 */
	NO_URL_PERMISSION(403, "请求资源权限不足。"),
	/**
	 * 4000登录失败
	 */
	LOGIN_FAILED(4000, "登录失败，账号或密码错误。"),


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


	RequestCodeConstant(Integer code, String description) {
		this.code = code;
		this.description = description;
	}
}
