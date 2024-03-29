package com.etd.framework.starter.client.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;


/**
 * Oauth2 身份验证服务配置器
 */
public class Oauth2AuthenticationConfigurer extends AbstractHttpConfigurer<Oauth2AuthenticationConfigurer, HttpSecurity> {


    private final Map<Class<? extends AbstractHttpSecurityConfigurer>, AbstractHttpSecurityConfigurer> configurers = Maps.newHashMap();


    private RequestMatcher endpointsMatcher;

    /**
     * 获取配置
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public <T extends AbstractHttpSecurityConfigurer> T getConfigurer(Class<T> clazz) {
        return (T) configurers.get(clazz);
    }

    /**
     * 添加配置项
     *
     * @param bean
     * @param <T>
     */
    public <T extends AbstractHttpSecurityConfigurer> void addConfigurer(T bean) {
        bean.setObjectPostProcessor(this::postProcess);
        configurers.put(bean.getClass(), bean);
    }


    @Override
    public void init(HttpSecurity builder) throws Exception {
        isEmpty();
        List<RequestMatcher> requestMatchers = Lists.newArrayList();
        configurers.forEach((key, configurer) -> {
            configurer.init(builder);
            if(!ObjectUtils.isEmpty(configurer.getRequestMatcher())){
                requestMatchers.add(configurer.getRequestMatcher());
            }
        });
        endpointsMatcher = new OrRequestMatcher(requestMatchers);


    }

    @Override
    public void configure(HttpSecurity builder) throws Exception {
        isEmpty();
        configurers.forEach((key, configurer) -> {
            configurer.configure(builder);
        });
    }


    /**
     * 判断配置项是否为空
     */
    private void isEmpty() {
        if (!CollectionUtils.isEmpty(configurers)) {
            return;
        }
        throw new NullPointerException("AbstractHttpSecurityConfigurer configurers is null");
    }

    /**
     * 获取端点匹配
     *
     * @return
     */
    public RequestMatcher getEndpointsMatcher() {
        return (request) -> this.endpointsMatcher.matches(request);
    }


}
