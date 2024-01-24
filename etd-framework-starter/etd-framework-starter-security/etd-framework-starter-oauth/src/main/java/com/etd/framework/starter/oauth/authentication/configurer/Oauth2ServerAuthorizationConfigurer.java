package com.etd.framework.starter.oauth.authentication.configurer;

import com.etd.framework.starter.oauth.authentication.configurer.extend.LoginAuthorizationConfigurer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;
import java.util.Map;

public class Oauth2ServerAuthorizationConfigurer extends AbstractHttpConfigurer<Oauth2ServerAuthorizationConfigurer, HttpSecurity> {

    /**
     * 默认的配置项
     */
    private final Map<Class<? extends AbstractAuthorizationConfigurer>, AbstractAuthorizationConfigurer> configurers = defaultConfigurers();

    private RequestMatcher endpointsMatcher;

    @Override
    public void init(HttpSecurity builder) throws Exception {
        List<RequestMatcher> requestMatchers = Lists.newArrayList();
        configurers.values().forEach(configurer -> {
            configurer.init(builder);
            requestMatchers.add(configurer.getRequestMatcher());
        });
        endpointsMatcher = new OrRequestMatcher(requestMatchers);
    }

    @Override
    public void configure(HttpSecurity builder) throws Exception {
        configurers.values().forEach(configurer -> {
            configurer.configure(builder);
        });
    }

    /**
     * 配置默认配置项
     *
     * @return
     */
    private Map<Class<? extends AbstractAuthorizationConfigurer>, AbstractAuthorizationConfigurer> defaultConfigurers() {
        Map<Class<? extends AbstractAuthorizationConfigurer>, AbstractAuthorizationConfigurer> configurerMap = Maps.newHashMap();
        configurerMap.put(LoginAuthorizationConfigurer.class, new LoginAuthorizationConfigurer(this::postProcess));
//        configurerMap.put(Oauth2AuthorizeEndpointConfigurer.class, new Oauth2AuthorizeEndpointConfigurer(this::postProcess));
//        configurerMap.put(Oauth2TokenEndpointConfigurer.class, new Oauth2TokenEndpointConfigurer(this::postProcess));
        return configurerMap;
    }

    /**
     * 向配置器内添加配置文件
     *
     * @param configurerType
     * @param configurer
     * @param <T>
     * @return
     */
    public <T extends AbstractAuthorizationConfigurer> Oauth2ServerAuthorizationConfigurer addConfigurer(Class<T> configurerType, AbstractAuthorizationConfigurer configurer) {
        configurers.put(configurerType, configurer);
        return this;
    }

    /**
     * 获取配置项
     *
     * @param configurerType
     * @param <T>
     * @return
     */
    public <T extends AbstractAuthorizationConfigurer> AbstractAuthorizationConfigurer getConfigurer(Class<T> configurerType) {
        return configurers.get(configurerType);
    }

    /**
     * 获取所有Configurer的请求匹配器
     *
     * @return
     */
    public RequestMatcher getEndpointsMatcher() {
        return (request) -> this.endpointsMatcher.matches(request);
    }

}
