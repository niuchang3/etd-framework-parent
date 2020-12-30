package org.edt.framework.starter.security.filter;

import org.apache.commons.lang3.StringUtils;
import org.edt.framework.starter.security.constants.SecurityConstants;
import org.edt.framework.starter.security.token.JwtToken;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 授权模式
 *
 * @author Young
 * @description
 * @date 2020/12/26
 */
@Order(0)
public class JwtAuthorizationFilter extends BasicAuthenticationFilter {


	private final StringRedisTemplate stringRedisTemplate;

	/**
	 * 最大重试次数
	 */
	private final static Integer maxRetry = 3;

	/**
	 * Creates an instance which will authenticate against the supplied
	 * {@code AuthenticationManager} and which will ignore failed authentication attempts,
	 * allowing the request to proceed down the filter chain.
	 *
	 * @param authenticationManager the bean to submit authentication requests to
	 */
	public JwtAuthorizationFilter(AuthenticationManager authenticationManager, StringRedisTemplate stringRedisTemplate) {
		super(authenticationManager);
		this.stringRedisTemplate = stringRedisTemplate;
	}


	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

		String token = request.getHeader(SecurityConstants.TOKEN_HEADER);
		//检查是否携带token信息
		if (StringUtils.isEmpty(token) || !token.startsWith(SecurityConstants.TOKEN_PREFIX)) {
			SecurityContextHolder.clearContext();
			chain.doFilter(request, response);
			return;
		}

		String tokenValue = token.replace(SecurityConstants.TOKEN_PREFIX, "");
		if (!StringUtils.isEmpty(tokenValue)) {
			tokenValue = tokenValue.trim();
		}


		String previousToken = "";
		Integer retryNumber = 0;
		while (retryNumber < maxRetry) {
			try {
				previousToken = stringRedisTemplate.opsForValue().get(JwtToken.getId(tokenValue));
				break;
			} catch (Exception e) {
				retryNumber++;
				try {
					Thread.sleep(1000 << retryNumber);
				} catch (InterruptedException e1) {
					throw e;
				}
			}

		}


		if (StringUtils.isEmpty(previousToken)) {
			SecurityContextHolder.clearContext();
			chain.doFilter(request, response);
			return;
		}
		UsernamePasswordAuthenticationToken authentication = JwtToken.getAuthentication(tokenValue);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		chain.doFilter(request, response);
	}
}
