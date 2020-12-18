package org.etd.framework.starter.web.interceptor.config;

import org.etd.framework.starter.web.interceptor.CustomInterceptor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Young
 * @description
 * @date 2020/11/12
 */
public class CustomDefaultInterceptor extends CustomInterceptor {
	/**
	 * 获取拦截器路径
	 *
	 * @return
	 */
	@Override
	public List<String> getInterceptorsPath() {
		List<String> list = new ArrayList<>();
		list.add("");
		return list;
	}
}
