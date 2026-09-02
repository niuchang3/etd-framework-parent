package com.etd.framework.starter.client.core.configurer;

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
 * 安全认证服务总配置器。
 * <p>
 * 聚合登录、刷新、Bearer 等子配置器，并交给 Spring Security 生命周期调用。
 */
public class SecurityAuthenticationConfigurer extends AbstractHttpConfigurer<SecurityAuthenticationConfigurer, HttpSecurity> {


    private final Map<Class<? extends AbstractSecurityEndpointConfigurer>, AbstractSecurityEndpointConfigurer> configurers = Maps.newHashMap();


    private RequestMatcher endpointsMatcher;

    /**
     * 获取指定类型的子配置器。
     *
     * @param clazz 子配置器类型
     * @param <T> 子配置器泛型
     * @return 子配置器实例
     */
    /**
     * 获取 Configurer 属性值
     *
     * @param clazz 参数 clazz
     * @return 处理结果
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractSecurityEndpointConfigurer> T getConfigurer(Class<T> clazz) {
        return (T) configurers.get(clazz);
    }

    /**
     * 添加子配置器。
     *
     * @param bean 子配置器实例
     * @param <T> 子配置器泛型
     */
    /**
     * 添加 Configurer
     *
     * @param bean 参数 bean
     * @return 处理结果
     */
    public <T extends AbstractSecurityEndpointConfigurer> void addConfigurer(T bean) {
        bean.setObjectPostProcessor(this::postProcess);
        configurers.put(bean.getClass(), bean);
    }


    /**
     * 初始化
     *
     * @param builder 参数 builder
     */
    @Override
    public void init(HttpSecurity builder) throws Exception {
        isEmpty();
        List<RequestMatcher> requestMatchers = Lists.newArrayList();
        configurers.forEach((key, configurer) -> {
            configurer.init(builder);
            if (!ObjectUtils.isEmpty(configurer.getRequestMatcher())) {
                requestMatchers.add(configurer.getRequestMatcher());
            }
        });
        endpointsMatcher = new OrRequestMatcher(requestMatchers);


    }

    /**
     * configure
     *
     * @param builder 参数 builder
     */
    @Override
    public void configure(HttpSecurity builder) throws Exception {
        isEmpty();
        configurers.forEach((key, configurer) -> configurer.configure(builder));
    }


    /**
     * 判断配置项是否为空
     */
    private void isEmpty() {
        if (!CollectionUtils.isEmpty(configurers)) {
            return;
        }
        throw new IllegalStateException("安全认证配置器不能为空。");
    }

    /**
     * 获取所有认证端点的组合匹配器。
     *
     * @return 认证端点匹配器
     */
    public RequestMatcher getEndpointsMatcher() {
        return (request) -> this.endpointsMatcher.matches(request);
    }


}
