package com.etd.framework.authentication.listening;

import com.etd.framework.authentication.event.AuthenticationSuccessEvent;
import org.springframework.context.ApplicationListener;

//@Component
public class AuthenticationSuccessListening implements ApplicationListener<AuthenticationSuccessEvent> {

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        System.out.print("AuthenticationSuccessEvent");
    }
}
