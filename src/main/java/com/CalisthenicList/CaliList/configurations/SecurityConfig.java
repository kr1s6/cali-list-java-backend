package com.CalisthenicList.CaliList.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
//    INFO  [OWASP] use a work factor of 10 or more and with a password limit of 72 bytes.
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
//        TODO change on Argon2id
    }
}
