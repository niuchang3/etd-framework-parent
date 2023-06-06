package com.etd.framework.authentication.event;

import org.springframework.context.ApplicationEvent;

public class AuthenticationSuccessEvent extends ApplicationEvent {

    private static final long serialVersionUID = -3269884896507308563L;

    public AuthenticationSuccessEvent(Object source) {
        super(source);
    }


}
