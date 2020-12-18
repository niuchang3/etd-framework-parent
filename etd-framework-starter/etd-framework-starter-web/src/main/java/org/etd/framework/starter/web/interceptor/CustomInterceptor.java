package org.etd.framework.starter.web.interceptor;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import java.util.List;

/**
 * @author Young
 * @description 自定义拦截器接口
 * @date 2020/11/12
 */

public abstract class CustomInterceptor extends HandlerInterceptorAdapter {

	/**
	 * 获取拦截器路径
	 * @return
	 */
	public abstract List<String> getInterceptorsPath();
}
