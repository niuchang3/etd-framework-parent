package org.edt.framework.starter.security.constants;

import lombok.Getter;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Young
 * @description
 * @date 2020/12/29
 */
public final class SecurityConstants {

	/**
	 * 访问白名单
	 */
	public static final String[] SWAGGER_WHITELIST = {
			"/swagger-ui.html",
			"/swagger-ui/*",
			"/swagger-resources/**",
			"/v2/api-docs",
			"/v3/api-docs",
			"/webjars/**"
	};

	/**
	 * 登录方式
	 */
	public enum LOGIN_MANNER {
		/**
		 * 账号密码登录
		 */
		PASSWORD("/api/auth/login", RequestMethod.POST);

		@Getter
		private String url;

		@Getter
		private RequestMethod requestMethod;

		LOGIN_MANNER(String url, RequestMethod requestMethod) {
			this.url = url;
			this.requestMethod = requestMethod;
		}
	}

	/**
	 * 需要过滤的的接口
	 */
	public static final String FILTER_ALL = "/api/**";

	// JWT token defaults
	public static final String TOKEN_HEADER = "Authorization";
	public static final String TOKEN_PREFIX = "Bearer ";
	public static final String TOKEN_TYPE = "JWT";

	public static final String JWT_TOKEN_USER = "USER";


	/**
	 * JWT签名密钥
	 */
	public static final String JWT_SECRET_KEY = "Q0iDoVU9FhlA7c0BO0TCE8q9xoJcd115aR+4HKCiSMY=";

	/**
	 * rememberMe 为 false 的时候过期时间是1个小时
	 */
	public static final long EXPIRATION = 60 * 60L;

	/**
	 * rememberMe 为 true 的时候过期时间是7天
	 */
	public static final long EXPIRATION_REMEMBER = 60 * 60 * 24 * 7L;
}
