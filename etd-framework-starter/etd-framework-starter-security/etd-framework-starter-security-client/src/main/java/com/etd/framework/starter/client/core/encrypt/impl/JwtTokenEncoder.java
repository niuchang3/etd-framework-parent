package com.etd.framework.starter.client.core.encrypt.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.etd.framework.starter.client.core.constant.SecurityParameterConstant;
import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import com.etd.framework.starter.client.core.token.TokenValue;
import org.etd.framework.common.core.user.UserDetails;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;

import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;


/**
 * JWT 令牌签发器。
 * <p>
 * 使用 RSA 私钥签名令牌，并把用户安全信息写入 JWT 载荷。
 */
public class JwtTokenEncoder implements TokenEncoder<Authentication, TokenValue> {


    private final JWSSigner jwsSigner;

    /**
     * 令牌签发配置。
     */
    private final SecurityProperties securityProperties;

    /**
     * 生成 JWT 唯一标识。
     */
    private final Snowflake snowflake;

    /**
     * 令牌编码器。
     *
     * @param privateKey         RSA 私钥
     * @param securityProperties 安全认证配置
     */
    public JwtTokenEncoder(PrivateKey privateKey, SecurityProperties securityProperties) {
        Assert.notNull(privateKey, "令牌签名私钥不能为空。");
        Assert.notNull(securityProperties, "安全配置不能为空。");
        this.jwsSigner = new RSASSASigner(privateKey);
        this.securityProperties = securityProperties;
        this.snowflake = IdUtil.getSnowflake(1, 1);
    }

    /**
     * encode
     *
     * @param tokenType 参数 tokenType
     * @param authentication 参数 authentication
     * @return 处理结果
     */
    @Override
    public TokenValue encode(SecurityParameterConstant.TokenType tokenType, Authentication authentication) {

        Assert.notNull(tokenType, "令牌类型不能为空。");
        Calendar now = getNow();
        Date signTime = now.getTime();
        // JWT 只放安全用户信息，不写入密码等敏感凭证。
        JWTClaimsSet build = new JWTClaimsSet.Builder()
                .jwtID(snowflake.nextIdStr())
                .issuer(securityProperties.getIssuer())
                .issueTime(signTime)
                .notBeforeTime(signTime)
                .expirationTime(getExpireTime(tokenType))
                .subject(String.valueOf(getUserDetails(authentication).getId()))
                .claim(Authentication.class.getName(), copySafeUserDetailsClaim(authentication))
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .customParam(SecurityParameterConstant.TokenType.class.getName(), tokenType.getCode())
                .build();

        SignedJWT signedJWT = new SignedJWT(header, build);
        try {
            signedJWT.sign(jwsSigner);
            String token = signedJWT.serialize();
            return new TokenValue(token, build.getExpirationTime().toInstant());
        } catch (JOSEException e) {
            throw new IllegalStateException("令牌签名失败。", e);
        }
    }


    private Calendar getNow() {
        return Calendar.getInstance();
    }

    /**
     * 从认证结果中获取业务用户详情。
     *
     * @param authentication 认证结果
     * @return 用户详情
     */
    private UserDetails getUserDetails(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof UserDetails userDetails)) {
            throw new IllegalArgumentException("认证用户信息不能为空。");
        }
        return userDetails;
    }

    /**
     * 复制可写入 JWT 的用户信息，避免把密码等敏感字段签入令牌。
     *
     * @param authentication 认证结果
     * @return 可安全写入令牌的用户信息
     */
    private UserDetails copySafeUserDetails(Authentication authentication) {
        UserDetails source = getUserDetails(authentication);
        UserDetails target = new UserDetails();
        target.setId(source.getId());
        target.setAccount(source.getAccount());
        target.setMobile(source.getMobile());
        target.setUserName(source.getUserName());
        target.setBirthday(source.getBirthday());
        target.setGender(source.getGender());
        target.setAvatar(source.getAvatar());
        target.setNickName(source.getNickName());
        target.setLocked(source.getLocked());
        target.setEnabled(source.getEnabled());
        target.setTenantId(source.getTenantId());
        target.setRoleCodes(source.getRoleCodes());
        target.setPlatformAdmin(source.getPlatformAdmin());
        target.setTenantAdmin(source.getTenantAdmin());
        target.setAuthorities(source.getAuthorities());
        target.setOrgId(source.getOrgId());
        target.setOrgIds(source.getOrgIds());
        target.setPermissionTypes(source.getPermissionTypes());
        target.setCustomOrgIds(source.getCustomOrgIds());
        target.setScopeOrgIds(source.getScopeOrgIds());
        return target;
    }

    /**
     * 复制可写入 JWT 的用户信息 Claim。
     *
     * @param authentication 认证结果
     * @return 可安全写入令牌的用户信息 Claim
     */
    private Map<String, Object> copySafeUserDetailsClaim(Authentication authentication) {
        UserDetails userDetails = copySafeUserDetails(authentication);
        Map<String, Object> claim = new java.util.LinkedHashMap<>();
        claim.put(SecurityParameterConstant.UserClaim.ID, userDetails.getId());
        claim.put(SecurityParameterConstant.UserClaim.ACCOUNT, userDetails.getAccount());
        claim.put(SecurityParameterConstant.UserClaim.MOBILE, userDetails.getMobile());
        claim.put(SecurityParameterConstant.UserClaim.USER_NAME, userDetails.getUserName());
        // 生日是无时区的业务日期，JWT 中使用 ISO 日期字符串避免跨时区后日期偏移。
        claim.put(SecurityParameterConstant.UserClaim.BIRTHDAY,
                userDetails.getBirthday() == null ? null : userDetails.getBirthday().toString());
        claim.put(SecurityParameterConstant.UserClaim.GENDER, userDetails.getGender());
        claim.put(SecurityParameterConstant.UserClaim.AVATAR, userDetails.getAvatar());
        claim.put(SecurityParameterConstant.UserClaim.NICK_NAME, userDetails.getNickName());
        claim.put(SecurityParameterConstant.UserClaim.LOCKED, userDetails.getLocked());
        claim.put(SecurityParameterConstant.UserClaim.ENABLED, userDetails.getEnabled());
        claim.put(SecurityParameterConstant.UserClaim.TENANT_ID, userDetails.getTenantId());
        claim.put(SecurityParameterConstant.UserClaim.ROLE_CODES, userDetails.getRoleCodes());
        claim.put(SecurityParameterConstant.UserClaim.PLATFORM_ADMIN, userDetails.getPlatformAdmin());
        claim.put(SecurityParameterConstant.UserClaim.TENANT_ADMIN, userDetails.getTenantAdmin());
        claim.put(SecurityParameterConstant.UserClaim.AUTHORITIES, userDetails.getAuthorities());
        claim.put(SecurityParameterConstant.UserClaim.ORG_ID, userDetails.getOrgId());
        claim.put(SecurityParameterConstant.UserClaim.ORG_IDS, userDetails.getOrgIds());
        claim.put(SecurityParameterConstant.UserClaim.PERMISSION_TYPES, userDetails.getPermissionTypes());
        claim.put(SecurityParameterConstant.UserClaim.CUSTOM_ORG_IDS, userDetails.getCustomOrgIds());
        claim.put(SecurityParameterConstant.UserClaim.SCOPE_ORG_IDS, userDetails.getScopeOrgIds());
        return claim;
    }

    /**
     * 根据配置换算过期时间。
     *
     * @param tokenType 令牌类型
     * @return 令牌过期时间
     */
    private Date getExpireTime(SecurityParameterConstant.TokenType tokenType) {
        SecurityProperties.Token token = null;
        if (SecurityParameterConstant.TokenType.ACCESS_TOKEN.equals(tokenType)) {
            token = securityProperties.getAccessToken();
        }
        if (SecurityParameterConstant.TokenType.REFRESH_TOKEN.equals(tokenType)) {
            token = securityProperties.getRefreshToken();
        }
        if (token == null || token.getExpired() == null || token.getTimeUnit() == null) {
            throw new IllegalArgumentException("令牌过期时间配置不能为空。");
        }

        Duration duration = Duration.of(token.getExpired(), token.getTimeUnit());
        Instant startInstant = Instant.now();
        Instant instant = startInstant.plusMillis(duration.toMillis());
        return Date.from(instant);
    }

}
