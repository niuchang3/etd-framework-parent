package org.etd.framework.starter.web;

import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.alibaba.fastjson.serializer.ValueFilter;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.etd.framework.starter.log.lnterceptor.TraceInterceptor;
import org.etd.framework.starter.web.interceptor.CustomInterceptor;
import org.etd.framework.starter.web.interceptor.config.CustomDefaultInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author Young
 * @description
 * @date 2020/9/2
 */
@Order(0)
@Configuration
@ComponentScan({"org.etd.framework.starter.web.*"})
public class WebAppConfig extends WebMvcConfigurationSupport {


	@Autowired
	private CustomInterceptor customInterceptor;


	@Bean
	@ConditionalOnMissingBean(CustomInterceptor.class)
	public CustomInterceptor initCustomInterceptor() {
		return new CustomDefaultInterceptor();
	}




	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
		FastJsonConfig config = new FastJsonConfig();

		//处理精度丢失的问题
		SerializeConfig serializeConfig = SerializeConfig.globalInstance;
		serializeConfig.put(BigInteger.class, ToStringSerializer.instance);
		serializeConfig.put(Long.class, ToStringSerializer.instance);
		serializeConfig.put(Long.TYPE, ToStringSerializer.instance);
		config.setSerializeConfig(serializeConfig);

		//null值转换为空字符串
		config.setSerializeFilters((ValueFilter) (object, name, value) -> {
			if (value == null) {
				return "";
			}
			return value;
		});

		config.setSerializerFeatures(
				// 保留 Map 空的字段
				SerializerFeature.WriteMapNullValue,
				// 将 String 类型的 null 转成""
				SerializerFeature.WriteNullStringAsEmpty,
				// 将 Number 类型的 null 转成 0
				SerializerFeature.WriteNullNumberAsZero,
				// 将 List 类型的 null 转成 []
				SerializerFeature.WriteNullListAsEmpty,
				// 将 Boolean 类型的 null 转成 false
				SerializerFeature.WriteNullBooleanAsFalse,
				// 将Date类型统一输出为：yyyy-MM-dd HH:mm:ss
				SerializerFeature.WriteDateUseDateFormat,
				// 避免循环引用
				SerializerFeature.DisableCircularReferenceDetect);

		converter.setFastJsonConfig(config);
		converter.setDefaultCharset(Charset.forName("UTF-8"));
		StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
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
		registry.addInterceptor(customInterceptor)
				.addPathPatterns(customInterceptor.getInterceptorsPath());
		super.addInterceptors(registry);
	}

	@Override
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
		super.addResourceHandlers(registry);
		registry.addResourceHandler("/**")
				.addResourceLocations("classpath:/static/");

		registry.addResourceHandler("swagger-ui.html")
				.addResourceLocations("classpath:/META-INF/resources/");

		registry.addResourceHandler("/webjars/**")
				.addResourceLocations("classpath:/META-INF/resources/webjars/");
	}
}
