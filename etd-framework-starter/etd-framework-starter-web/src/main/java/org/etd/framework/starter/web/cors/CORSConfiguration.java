package org.etd.framework.starter.web.cors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * 访问跨域问题
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CORSConfiguration {

	/**
	 * 从配置文件读取项目使用的环境（dev，pord，test等）,默认值：空字符串
	 */
	@Value("${spring.profiles.active:}")
	private String active;

	//全局配置跨域
	@Bean("corsConfigurer")
	@Profile({"local", "qa", "dev"})
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurerAdapter() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
						.allowCredentials(true).maxAge(3600)
						.allowedOrigins("*")
						.allowedMethods("*").allowedHeaders("*");
			}
		};
	}

	@Bean
	public CorsStatus corsClose() {
		CorsStatus cs = new CorsStatus();
		cs.setActive(active);
		if ("local".equals(active) || "qa".equals(active) || "dev".equals(active)) {
			cs.setEnable(true);
		} else {
			cs.setEnable(false);
		}
		return cs;
	}


}
