package com.etd.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests( authorizeRequests  ->{
            authorizeRequests.anyRequest().authenticated();
        }).formLogin(Customizer.withDefaults());
        return http.build();
    }

    /**
     * Security配置用户信息
     *
     * @return
     */
    @Bean
    public UserDetailsService userDetailsService() {

        UserDetails userDetails = User.withDefaultPasswordEncoder()
                .username("zhangsan")
                .password("123456")
                .roles("ADMIN")
                .authorities("READ", "WRITE")
                .build();
        return new InMemoryUserDetailsManager(userDetails);
    }
}
