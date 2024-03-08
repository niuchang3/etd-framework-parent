package com.etd.framework.starter.oauth.authentication.password.provider;

import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.client.core.user.UserDetails;
import com.etd.framework.starter.client.core.token.UserPasswordAuthenticationRequestToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.ObjectUtils;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserPasswordAuthenticationProvider implements AuthenticationProvider {


    private IUserService userService;

    private PasswordEncoder passwordEncoder;


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        validata(authentication);
        UserDetails userDetails = userService.loadUserByAccount((String) authentication.getPrincipal());
        validata(userDetails, authentication);
        return converterAuthentication(userDetails);
    }


    /**
     * 验证请求信息
     *
     * @param authentication
     */
    private void validata(Authentication authentication) {
        if (ObjectUtils.isEmpty(authentication.getPrincipal())) {
            throw new UsernameNotFoundException("用户名不能为空。");
        }
        if (ObjectUtils.isEmpty(authentication.getCredentials())) {
            throw new BadCredentialsException("密码不能为空。");
        }
    }


    private void validata(UserDetails userDetails, Authentication authentication) {
        if (ObjectUtils.isEmpty(userDetails)) {
            throw new UsernameNotFoundException("错误的用户名。");
        }

        boolean matches = passwordEncoder.matches((CharSequence) authentication.getCredentials(), userDetails.getPassword());
        if (!matches) {
            throw new BadCredentialsException("错误的密码凭证。");
        }
        if (!userDetails.getEnabled()) {
            throw new DisabledException("账号已被禁用。");
        }
        if (userDetails.getLocked()) {
            throw new LockedException("账号已被锁定,请联系管理员解锁。");
        }
    }

    /**
     * 用户详情转换为Authentication
     *
     * @param userDetails
     * @return
     */
    private Authentication converterAuthentication(UserDetails userDetails) {
        UserPasswordAuthenticationRequestToken token = new UserPasswordAuthenticationRequestToken(userDetails.getAuthorities());
        token.setUsername(userDetails.getUserName());
        token.setDetails(userDetails);
        token.setAuthenticated(true);
        return token;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UserPasswordAuthenticationRequestToken.class.isAssignableFrom(authentication);
    }


}
