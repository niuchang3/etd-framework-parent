package com.etd.framework.authentication.listening;

import com.etd.framework.authentication.event.AuthenticationFailEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

//@Component
public class AuthenticationFailListening implements ApplicationListener<AuthenticationFailEvent> {

    @Override
    public void onApplicationEvent(AuthenticationFailEvent event) {
        System.out.print("AuthenticationFailEvent");
    }
}
