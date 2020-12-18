package org.etd.framework.starter.web.swagger;

import springfox.documentation.service.ApiInfo;

/**
 * @author Young
 * @description
 * @date 2020/4/28
 */
public interface INewSwaggerConfig {

	/**
	 * 获取swagger扫描路径,swagger将只扫描这个路径下的api
	 *
	 * @return
	 */
	public String getScanPackage();

	default String getGroupName() {
		return "default";
	}
}
