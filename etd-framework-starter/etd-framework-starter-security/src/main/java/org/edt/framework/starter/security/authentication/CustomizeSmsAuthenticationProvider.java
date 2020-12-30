package org.edt.framework.starter.security.authentication;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @author Young
 * @description
 * @date 2020/12/26
 */

public class CustomizeSmsAuthenticationProvider implements AuthenticationProvider {


	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		authentication.getName();
		String smsCode = authentication.getCredentials().toString();
		String mobile = authentication.getPrincipal().toString();

		throw new BadCredentialsException("账号密码正确");
	}


	@Override
	public boolean supports(Class<?> authentication) {
//		return (MobileAuthenticationToken.class
//				.isAssignableFrom(authentication));
		return true;
	}
}
