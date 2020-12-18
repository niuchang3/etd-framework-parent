package org.etd.framework.starter.web.cors;

import lombok.Getter;
import lombok.Setter;

/**
 * 跨域功能是否开启，该类由CORSConfiguration类创建并管理
 *
 * @author Young
 * @date 2019/10/29 9:24
 * @see CORSConfiguration
 */
@Getter
@Setter
public class CorsStatus {
	/**
	 * 是否启用跨域配置
	 */
	private boolean enable;
	/**
	 * 当前启用的环境
	 */
	private String active;
}
