package com.etd.framework.authentication;

import com.etd.framework.authentication.configuration.UserAuthenticationConfigurer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;
import java.util.Map;

/**
 * 基本的身份认证配置器
 */
public class BasicAuthenticationConfiguration extends AbstractHttpConfigurer<BasicAuthenticationConfiguration, HttpSecurity> {

    private final Map<Class<? extends AbstractBasicAuthenticationConfigurer>, AbstractBasicAuthenticationConfigurer> configurers = defaultConfig();

    private RequestMatcher endpointsMatcher;

    /**
     * 创建默认的
     *
     * @return
     */
    protected Map<Class<? extends AbstractBasicAuthenticationConfigurer>, AbstractBasicAuthenticationConfigurer> defaultConfig() {
        Map<Class<? extends AbstractBasicAuthenticationConfigurer>, AbstractBasicAuthenticationConfigurer> configurers = Maps.newLinkedHashMap();
        configurers.put(UserAuthenticationConfigurer.class, new UserAuthenticationConfigurer(this::postProcess));
        return configurers;
    }

    /**
     * 定制 UserAuthenticationConfigurer
     *
     * @param customizer
     * @return
     */
    private BasicAuthenticationConfiguration customizerUserAuthenticationConfigurer(Customizer<UserAuthenticationConfigurer> customizer) {
        customizer.customize(getConfigurer(UserAuthenticationConfigurer.class));
        return this;
    }


    private <T> T getConfigurer(Class<T> type) {
        return (T) this.configurers.get(type);
    }

    /**
     * 添加配置信息
     *
     * @param type
     * @param configurer
     */
    public void addConfigurer(Class<? extends AbstractBasicAuthenticationConfigurer> type, AbstractBasicAuthenticationConfigurer configurer) {
        this.configurers.put(type, configurer);
    }

    /**
     * 调用初始化方法
     *
     * @param httpSecurity
     * @throws Exception
     */
    @Override
    public void init(HttpSecurity httpSecurity) throws Exception {
        List<RequestMatcher> requestMatchers = Lists.newArrayList();
        configurers.values().forEach(config -> {
            config.init(httpSecurity);
            requestMatchers.add(config.getRequestMatcher());
        });
        RequestMatcher request = new OrRequestMatcher(requestMatchers);
        httpSecurity.csrf().ignoringRequestMatchers(request);
    }

    @Override
    public void configure(HttpSecurity httpSecurity) {
        configurers.values().forEach(config -> config.configure(httpSecurity));
    }

}
