package com.source.code.reading.spring;


import com.source.code.reading.spring.config.SpringConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SpringSourceReading {


    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);

        Object testBean = context.getBean("testBean");
        System.out.println(testBean);
        System.out.println(111);
    }


}
