package com.CalisthenicList.CaliList.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
// INFO should be used for @Bean definition and configuration
public class SecurityConfig {
	private static final int saltLength = 16;
	private static final int hashLength = 32;
	private static final int parallelism = 1;
	private static final int memory = 12_288;
	private static final int iterations = 3;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new Argon2PasswordEncoder(saltLength, hashLength, parallelism, memory, iterations);
	}
}
