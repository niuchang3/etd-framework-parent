package com.etd.framework.utils;

import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.util.ObjectUtils;

public class SharedObjectUtil {


    public static <T> T getObject(HttpSecurity httpSecurity, Class<T> tClass) {
        T object = httpSecurity.getSharedObject(tClass);
        if (ObjectUtils.isEmpty(object)) {
            ApplicationContext context = httpSecurity.getSharedObject(ApplicationContext.class);
            return context.getBean(tClass);
        }
        return null;
    }

    public static void getObject(Class<AuthenticationManager> authenticationManagerClass) {
    }
}
