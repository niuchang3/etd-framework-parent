package org.etd.framework.starter.web.swagger;

import lombok.extern.slf4j.Slf4j;
import org.etd.framework.common.utils.SpringContextHelper;
import org.etd.framework.starter.web.swagger.config.DefaultSwaggerConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.List;

/**
 * swagger配置
 *
 * @author Young
 * @date 2018-10-22
 */
@Slf4j
@Configuration
@EnableSwagger2
public class SwaggerConfig implements InitializingBean , ApplicationContextAware {


	@Bean
	@ConditionalOnMissingBean(INewSwaggerConfig.class)
	public INewSwaggerConfig apiInfo() {
		return new DefaultSwaggerConfig();
	}

	@Autowired
	private List<INewSwaggerConfig> swaggerConfigs;


	@Override
	public void afterPropertiesSet() {

		DefaultListableBeanFactory bf = (DefaultListableBeanFactory) getApplicationContext().getAutowireCapableBeanFactory();
		for (INewSwaggerConfig sc : swaggerConfigs) {
			if (sc instanceof DefaultSwaggerConfig) {
				continue;
			}
			if (log.isDebugEnabled()) {
				log.debug("注册SwaggerConfig = {},扫描controller路径 = {}", sc.getClass().getSimpleName(), sc.getScanPackage());
			}
			bf.registerSingleton(sc.getClass().getSimpleName(), new Docket(DocumentationType.SWAGGER_2)
					.apiInfo(initializeSwaggerAppInfo())
					.groupName(sc.getGroupName())
					.select()
					.apis(RequestHandlerSelectors.basePackage(sc.getScanPackage()))
					.paths(PathSelectors.any())
					.build());
		}
	}


	private final ApiInfo initializeSwaggerAppInfo() {
		return new ApiInfoBuilder()
				.title("api文档")
				.description("swagger2接口文档")
				.termsOfServiceUrl("")
				.version("1.0.0")
				.license("Apache 2.0")
				.licenseUrl("http://www.apache.org/licenses/LICENSE-2.0")
				.contact(new Contact("", "", ""))
				.build();
	}








	private static ApplicationContext context = null;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}


	public static Object getBean(String name) {
		return context.getBean(name);
	}


	public static Object getBean(Class<?> beanClass) {
		return context.getBean(beanClass);
	}

	public static ApplicationContext getApplicationContext() {
		return context;
	}
}