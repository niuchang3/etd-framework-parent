package org.etd.framework.starter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.starter.log.interceptor.TraceInterceptor;
import org.etd.framework.starter.web.interceptor.EtdFrameworkHttpRequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Young
 * @description
 * @date 2020/9/2
 */
@Order(0)
@Configuration
@ComponentScan({"org.etd.framework.starter.web"})
public class WebAppConfig implements WebMvcConfigurer {


	@Autowired
	private EtdFrameworkHttpRequestInterceptor etdFrameworkHttpRequestInterceptor;

	@Autowired
	private ObjectMapper objectMapper;



	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
		List<MediaType> supportedMediaTypes = new ArrayList<>();
		supportedMediaTypes.add(MediaType.APPLICATION_JSON);
		supportedMediaTypes.add(MediaType.APPLICATION_ATOM_XML);
		supportedMediaTypes.add(MediaType.APPLICATION_FORM_URLENCODED);
		supportedMediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
		supportedMediaTypes.add(MediaType.APPLICATION_PDF);
		supportedMediaTypes.add(MediaType.APPLICATION_RSS_XML);
		supportedMediaTypes.add(MediaType.APPLICATION_XHTML_XML);
		supportedMediaTypes.add(MediaType.APPLICATION_XML);
		supportedMediaTypes.add(MediaType.IMAGE_GIF);
		supportedMediaTypes.add(MediaType.IMAGE_JPEG);
		supportedMediaTypes.add(MediaType.IMAGE_PNG);
		supportedMediaTypes.add(MediaType.TEXT_EVENT_STREAM);
		supportedMediaTypes.add(MediaType.TEXT_HTML);
		supportedMediaTypes.add(MediaType.TEXT_MARKDOWN);
		supportedMediaTypes.add(MediaType.TEXT_PLAIN);
		supportedMediaTypes.add(MediaType.TEXT_XML);


		converter.setSupportedMediaTypes(supportedMediaTypes);


		StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
		converters.add(stringConverter);
		converters.add(converter);
	}

	/**
	 * 自行注册拦截器
	 *
	 * @param registry
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new TraceInterceptor())
				.addPathPatterns("/**");

		registry.addInterceptor(etdFrameworkHttpRequestInterceptor)
				.addPathPatterns(etdFrameworkHttpRequestInterceptor.getInterceptorsPath());
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
				.addResourceLocations("classpath:/static/");

		registry.addResourceHandler("swagger-ui.html")
				.addResourceLocations("classpath:/META-INF/resources/");

		registry.addResourceHandler("/webjars/**")
				.addResourceLocations("classpath:/META-INF/resources/webjars/");
	}


}
