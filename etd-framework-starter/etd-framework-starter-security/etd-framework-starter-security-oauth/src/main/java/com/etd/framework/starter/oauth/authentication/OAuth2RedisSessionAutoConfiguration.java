package com.etd.framework.starter.oauth.authentication;

import com.etd.framework.starter.oauth.authentication.oauth2.properties.OAuth2SessionProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * OAuth2授权中心Redis Session配置。
 */
@AutoConfiguration
@ConditionalOnClass({RedisSessionRepository.class, RedisConnectionFactory.class})
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnProperty(prefix = "system.security.oauth2.session", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OAuth2SessionProperties.class)
public class OAuth2RedisSessionAutoConfiguration {

    /**
     * 定制普通Redis Session仓储。
     *
     * @param properties OAuth2网页登录态配置
     * @return Session仓储定制器
     */
    @Bean
    public SessionRepositoryCustomizer<RedisSessionRepository> redisSessionRepositoryCustomizer(OAuth2SessionProperties properties) {
        return repository -> {
            // 通过项目自己的配置入口控制Redis Session过期时间和命名空间。
            repository.setDefaultMaxInactiveInterval(properties.getTimeout());
            repository.setRedisKeyNamespace(properties.getNamespace());
        };
    }

    /**
     * 定制OAuth2授权中心Session Cookie。
     *
     * @param properties OAuth2网页登录态配置
     * @return Cookie序列化器
     */
    @Bean
    @ConditionalOnMissingBean(CookieSerializer.class)
    public CookieSerializer oauth2CookieSerializer(OAuth2SessionProperties properties) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(properties.getCookieName());
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite(properties.getSameSite());
        serializer.setCookiePath("/");
        return serializer;
    }

    /**
     * Spring Session专用Redis序列化器。
     * <p>
     * Bean名称使用Spring Session约定值，只影响Session属性序列化，不覆盖业务RedisTemplate。
     *
     * @return Spring Session Redis序列化器
     */
    @Bean("springSessionDefaultRedisSerializer")
    @ConditionalOnMissingBean(name = "springSessionDefaultRedisSerializer")
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Session里包含Spring Security认证对象，使用官方Jackson模块保证认证信息可反序列化。
        ClassLoader classLoader = getClass().getClassLoader();
        SecurityJackson2Modules.enableDefaultTyping(objectMapper);
        objectMapper.registerModules(SecurityJackson2Modules.getModules(classLoader));
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(new JavaTimeModule());
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
