package org.etd.framework.starter.web.swagger.config;

import org.etd.framework.starter.web.swagger.INewSwaggerConfig;

public class DefaultSwaggerConfig implements INewSwaggerConfig {

	@Override
	public String getScanPackage() {
		return "";
	}

	@Override
	public String getGroupName() {
		return "";
	}
}
