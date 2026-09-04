package com.etd.framework.starter.oauth.authentication.internal.provider;

import com.etd.framework.starter.client.core.i18n.SecurityMessageCode;
import com.etd.framework.starter.oauth.authentication.internal.token.UserPasswordAuthenticationRequestToken;
import com.etd.framework.starter.client.core.user.IUserService;
import org.etd.framework.common.core.user.UserDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
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



    @Setter(onMethod_ = @Autowired)
    private IUserService userService;

    @Setter(onMethod_ = @Autowired)
    private PasswordEncoder passwordEncoder;


    /**
     * authenticate
     *
     * @param authentication 参数 authentication
     * @return 处理结果
     */
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
            throw new UsernameNotFoundException(SecurityMessageCode.USER_NOT_FOUND);
        }
        if (ObjectUtils.isEmpty(authentication.getCredentials())) {
            throw new BadCredentialsException(SecurityMessageCode.PASSWORD_INVALID);
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
            throw new UsernameNotFoundException(SecurityMessageCode.USER_NOT_FOUND);
        }
        if (ObjectUtils.isEmpty(userDetails.getPassword())) {
            throw new BadCredentialsException(SecurityMessageCode.PASSWORD_INVALID);
        }
        boolean matches = passwordEncoder.matches((CharSequence) authentication.getCredentials(), userDetails.getPassword());
        if (!matches) {
            throw new BadCredentialsException(SecurityMessageCode.PASSWORD_INVALID);
        }
        if (!userDetails.isLoginEnabled()) {
            throw new DisabledException(SecurityMessageCode.ACCOUNT_DISABLED);
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

    /**
     * supports
     *
     * @param authentication 参数 authentication
     * @return 处理结果
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return UserPasswordAuthenticationRequestToken.class.isAssignableFrom(authentication);
    }


}
