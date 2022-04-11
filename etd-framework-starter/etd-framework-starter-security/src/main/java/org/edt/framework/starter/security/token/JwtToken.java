package org.edt.framework.starter.security.token;

import cn.hutool.json.JSONUtil;
import org.edt.framework.starter.security.constants.SecurityConstants;
import org.edt.framework.starter.security.entity.UserDetailModel;
import org.edt.framework.starter.security.utils.UserDetailUtils;
import org.etd.framework.starter.cache.utils.Redis;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.CollectionUtils;

import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Young
 * @description
 * @date 2020/12/29
 */
public class JwtToken {


    /**
     * 生成足够的安全随机密钥，以适合符合规范的签名
     */
    private static final byte[] API_KEY_SECRET_BYTES = DatatypeConverter.parseBase64Binary(SecurityConstants.JWT_SECRET_KEY);


    /**
     * 生成token
     *
     * @param userDetailModel
     * @param isRememberMe
     * @return
     */
    public static String createToken(StringRedisTemplate redisTemplate, UserDetailModel userDetailModel, boolean isRememberMe) {
        long expiration = isRememberMe ? SecurityConstants.EXPIRATION_REMEMBER : SecurityConstants.EXPIRATION;
        final Date createdDate = new Date();
        final Date expirationDate = new Date(createdDate.getTime() + expiration * 1000);
        final String keyId = Redis.genKey(SecurityConstants.TOKEN_TYPE, userDetailModel.getId());


        return SecurityConstants.TOKEN_PREFIX;
    }

    /**
     * 删除Token信息
     *
     * @param redisTemplate
     */
    public static void removeToken(StringRedisTemplate redisTemplate) {
        UserDetailModel userDetail = UserDetailUtils.getUserDetail();
        final String keyId = Redis.genKey(SecurityConstants.TOKEN_TYPE, userDetail.getId());
        redisTemplate.delete(keyId);
    }


    /**
     * 根据token解析ID信息
     *
     * @param token
     * @return
     */
//    public static String getId(String token) {
//        Claims claims = getClaims(token);
//        return claims.getId();
//    }

    /**
     * JWT转 SecurityToken
     *
     * @return
     */
//    public static UsernamePasswordAuthenticationToken getAuthentication(String token) {
//        Claims claims = getClaims(token);
//        UserDetailModel detailModel = conversionBean(claims);
//        return new UsernamePasswordAuthenticationToken(detailModel, token, conversionGrantedAuthority(detailModel.getPermissions()));
//    }

    /**
     * 对token进行解密
     *
     * @param token
     * @return
     */
//    private static Claims getClaims(String token) {
//        return Jwts.parser()
//                .setSigningKey(SECRET_KEY)
//                .parseClaimsJws(token)
//                .getBody();
//    }

    /**
     * 获取权限信息
     *
     * @return
     */
//    private static List<SimpleGrantedAuthority> getPermission(Claims claims) {
//        UserDetailModel detailModel = conversionBean(claims);
//        return detailModel.getPermissions().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
//    }

    /**
     * 将用户详情转换为JSON字符串
     *
     * @param userDetailModel
     * @return
     */
    private static String conversionJson(UserDetailModel userDetailModel) {
        return JSONUtil.toJsonStr(userDetailModel);
    }

    /**
     * 从JWT中解析用户详情
     *
     * @param claims
     * @return
     */
//    private static UserDetailModel conversionBean(Claims claims) {
//        String jsonStr = (String) claims.get(SecurityConstants.JWT_TOKEN_USER);
//        return JSONUtil.toBean(jsonStr, UserDetailModel.class);
//    }

    /**
     * 转换授权信息
     *
     * @param permissions
     * @return
     */
    private static List<GrantedAuthority> conversionGrantedAuthority(List<String> permissions) {
        if (CollectionUtils.isEmpty(permissions)) {
            return new ArrayList<>();
        }
        return permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

}
