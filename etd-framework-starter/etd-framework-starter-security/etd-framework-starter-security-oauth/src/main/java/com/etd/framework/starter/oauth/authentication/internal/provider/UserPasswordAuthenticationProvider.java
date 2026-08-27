package com.etd.framework.starter.oauth.authentication.internal.provider;

import com.etd.framework.starter.oauth.authentication.internal.token.UserPasswordAuthenticationRequestToken;
import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.client.core.user.UserDetails;
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
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 用户名密码认证提供者。
 * <p>
 * 负责校验账号、密码和账号状态，认证成功后返回携带用户详情的认证对象。
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserPasswordAuthenticationProvider implements AuthenticationProvider {


    private IUserService userService;

    private PasswordEncoder passwordEncoder;


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        Assert.notNull(userService, "用户服务不能为空。");
        Assert.notNull(passwordEncoder, "密码编码器不能为空。");
        validate(authentication);
        UserDetails userDetails = userService.loadUserByAccount((String) authentication.getPrincipal());
        validate(userDetails, authentication);
        return converterAuthentication(userDetails);
    }


    /**
     * 验证登录请求信息。
     *
     * @param authentication 认证请求
     */
    private void validate(Authentication authentication) {
        if (ObjectUtils.isEmpty(authentication.getPrincipal())) {
            throw new UsernameNotFoundException("用户名不能为空。");
        }
        if (ObjectUtils.isEmpty(authentication.getCredentials())) {
            throw new BadCredentialsException("密码不能为空。");
        }
    }

    /**
     * 验证用户和账号状态。
     *
     * @param userDetails 用户详情
     * @param authentication 认证请求
     */
    private void validate(UserDetails userDetails, Authentication authentication) {
        if (ObjectUtils.isEmpty(userDetails)) {
            throw new UsernameNotFoundException("用户名错误。");
        }
        if (ObjectUtils.isEmpty(userDetails.getPassword())) {
            throw new BadCredentialsException("用户密码未初始化。");
        }
        boolean matches = passwordEncoder.matches((CharSequence) authentication.getCredentials(), userDetails.getPassword());
        if (!matches) {
            throw new BadCredentialsException("密码错误。");
        }
        if (!Boolean.TRUE.equals(userDetails.getEnabled())) {
            throw new DisabledException("账号已被禁用。");
        }
        if (Boolean.TRUE.equals(userDetails.getLocked())) {
            throw new LockedException("账号已被锁定，请联系管理员解锁。");
        }
    }

    /**
     * 用户详情转换为认证结果。
     *
     * @param userDetails 用户详情
     * @return 认证结果
     */
    private Authentication converterAuthentication(UserDetails userDetails) {
        UserPasswordAuthenticationRequestToken token = new UserPasswordAuthenticationRequestToken(userDetails.getAuthorities());
        token.setUsername(String.valueOf(userDetails.getId()));
        token.setDetails(userDetails);
        token.setAuthenticated(true);
        return token;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UserPasswordAuthenticationRequestToken.class.isAssignableFrom(authentication);
    }


}
