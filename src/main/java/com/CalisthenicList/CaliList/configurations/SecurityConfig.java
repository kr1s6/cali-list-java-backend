package com.CalisthenicList.CaliList.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

@Configuration
// INFO should be used for @Bean definition and configuration
public class SecurityConfig {
	@Bean
	public Argon2PasswordEncoder passwordEncoder() {
		return new Argon2PasswordEncoder(16, 32, 1, 12288, 3);
	}
}
