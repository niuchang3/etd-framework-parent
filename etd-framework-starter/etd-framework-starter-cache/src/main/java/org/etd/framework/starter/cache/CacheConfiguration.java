package org.etd.framework.starter.cache;


import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author Young
 * @description
 * @date 2020/7/25
 */
@Configuration
public class CacheConfiguration {

	@Autowired
	private RedisConnectionFactory factory;


	@Bean
	public RedisTemplate redisTemplate() {
		RedisTemplate redisTemplate = new RedisTemplate();
		redisTemplate.setConnectionFactory(factory);
		return redisTemplate;
	}

	@Bean
	CacheManager cacheManager(RedissonClient redissonClient) {
		return new RedissonSpringCacheManager(redissonClient);
	}
}
