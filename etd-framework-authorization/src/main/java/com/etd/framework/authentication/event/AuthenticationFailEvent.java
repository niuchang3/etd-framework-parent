package com.etd.framework.authentication.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class AuthenticationFailEvent extends ApplicationEvent {

    @Getter
    private Exception exception;

    public AuthenticationFailEvent(Object source, Exception exception) {
        super(source);
        this.exception = exception;
    }
}
