package org.edt.framework.starter.security;

import org.edt.framework.starter.security.constants.SecurityConstants;
import org.edt.framework.starter.security.filter.JwtAuthorizationFilter;
import org.edt.framework.starter.security.handler.JwtAccessDeniedHandler;
import org.edt.framework.starter.security.handler.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static java.util.Collections.singletonList;

/**
 * @author Young
 * @description
 * @date 2020/12/26
 */
@Configuration
@ComponentScan({"org.edt.framework.starter.security.*"})
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityWebConfig extends WebSecurityConfigurerAdapter {

	private final StringRedisTemplate stringRedisTemplate;


	public SecurityWebConfig(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}


	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		http.cors(Customizer.withDefaults())
				.csrf().disable()
				.authorizeRequests()
				// 开放Swagger & 登录认证资源不需要认证
				.antMatchers(SecurityConstants.SWAGGER_WHITELIST).permitAll()
				.antMatchers(SecurityConstants.LOGIN_MANNER.PASSWORD.getRequestMethod().name(), SecurityConstants.LOGIN_MANNER.PASSWORD.getUrl()).permitAll()
				// 以/api/**下的所有接口全部需要认证
				.antMatchers(SecurityConstants.FILTER_ALL).authenticated()
				// 其他资源全部放行
				.anyRequest().permitAll()

				.and()
				.addFilter(new JwtAuthorizationFilter(authenticationManager(), stringRedisTemplate))
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
				.exceptionHandling().authenticationEntryPoint(new JwtAuthenticationEntryPoint())
				.accessDeniedHandler(new JwtAccessDeniedHandler())
				.and()
				.headers().frameOptions().disable();


	}


	/**
	 * Cors配置优化
	 **/
	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(singletonList("*"));
		// configuration.setAllowedOriginPatterns(singletonList("*"));
		configuration.setAllowedHeaders(singletonList("*"));
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "DELETE", "PUT", "OPTIONS"));
		configuration.setExposedHeaders(singletonList(SecurityConstants.TOKEN_HEADER));
		configuration.setAllowCredentials(false);
		configuration.setMaxAge(3600l);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
