package com.source.code.reading.spring.bean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestAutowired {

    @Autowired
    private TestBean testBean;
}
