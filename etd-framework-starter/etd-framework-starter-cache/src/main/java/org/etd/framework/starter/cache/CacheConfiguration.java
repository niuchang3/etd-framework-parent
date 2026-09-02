package org.etd.framework.starter.cache;


import com.alicp.jetcache.anno.config.EnableMethodCache;
import com.alicp.jetcache.support.Fastjson2ValueDecoder;
import com.alicp.jetcache.support.Fastjson2ValueEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author Young
 * @description
 * @date 2020/7/25
 */
@Configuration
@ComponentScan("org.etd.framework.starter.cache.**")
@EnableMethodCache(basePackages = "org.etd")
public class CacheConfiguration {


    /**
     * 配置RedisTemplate 序列化相关规则
     *
     * @param factory
     * @return
     */
    /**
     * redis Template
     *
     * @param factory 参数 factory
     * @return 处理结果
     */
    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory factory) {
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(new JavaTimeModule());
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        RedisTemplate<String,Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);

        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.afterPropertiesSet();


        return redisTemplate;
    }


    /**
     * caffeine Cache
     *
     * @return 处理结果
     */
    @Bean
    @ConditionalOnMissingBean(Cache.class)
    public Cache<String, Object> caffeineCache() {

        return Caffeine.newBuilder()
                .initialCapacity(128)//初始大小
                .maximumSize(2048)//最大数量
                .expireAfterWrite(2, TimeUnit.SECONDS)//过期时间
                .build();
    }

    /**
     * jet Cache Fastjson Value Encoder
     *
     * @return 处理结果
     */
    @Bean
    @ConditionalOnMissingBean(name = "jetCacheFastjson2ValueEncoder")
    public Function<Object, byte[]> jetCacheFastjson2ValueEncoder() {
        return Fastjson2ValueEncoder.INSTANCE;
    }

    /**
     * jet Cache Fastjson Value Decoder
     *
     * @return 处理结果
     */
    @Bean
    @ConditionalOnMissingBean(name = "jetCacheFastjson2ValueDecoder")
    public Function<byte[], Object> jetCacheFastjson2ValueDecoder() {
        return Fastjson2ValueDecoder.INSTANCE;
    }

    /**
     * redis Utils
     *
     * @param Cache<String 参数 Cache<String
     * @param caffeineCache 参数 caffeineCache
     * @param @Qualifier("redisTemplate" 参数 @Qualifier("redisTemplate"
     * @return 处理结果
     */
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    public RedisCache redisUtils(Cache<String, Object> caffeineCache, @Qualifier("redisTemplate") RedisTemplate<String,Object> redisTemplate) {
        return new RedisCache(caffeineCache, redisTemplate);
    }

}
