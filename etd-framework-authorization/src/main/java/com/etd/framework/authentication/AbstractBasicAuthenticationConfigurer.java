package com.etd.framework.authentication;

import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.util.matcher.RequestMatcher;

public abstract class AbstractBasicAuthenticationConfigurer {

    private final ObjectPostProcessor<Object> objectPostProcessor;


    public AbstractBasicAuthenticationConfigurer(ObjectPostProcessor<Object> objectPostProcessor) {
        this.objectPostProcessor = objectPostProcessor;
    }

    public abstract void init(HttpSecurity httpSecurity);

    public abstract void configure(HttpSecurity httpSecurity);

    public abstract RequestMatcher getRequestMatcher();

    protected final <T> T postProcess(T object) {
        return (T) this.objectPostProcessor.postProcess(object);
    }

    protected final ObjectPostProcessor<Object> getObjectPostProcessor() {
        return this.objectPostProcessor;
    }

    protected ApplicationContext getContext(HttpSecurity httpSecurity) {
        return httpSecurity.getSharedObject(ApplicationContext.class);
    }

}
