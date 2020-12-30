package org.edt.framework.starter.security.component;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * @author Young
 * @description
 * @date 2020/12/26
 */
@Component
public class UserDetailServiceImpl implements UserDetailsService {


	@Override
	public UserDetails loadUserByUsername(String account) throws UsernameNotFoundException {
		return null;
	}
}
