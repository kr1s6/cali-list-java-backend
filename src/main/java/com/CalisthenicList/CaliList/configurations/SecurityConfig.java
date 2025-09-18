package com.CalisthenicList.CaliList.configurations;

import com.CalisthenicList.CaliList.filter.UserValidationRateLimitingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
// INFO - Configuration should be used for @Bean definition
// TODO - Potentially add GlobalRateLimiter
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

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, UserValidationRateLimitingFilter userValidationRateLimitingFilter)
			throws Exception {
		return http
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				// Register the rate-limiting filter before the UsernamePasswordAuthenticationFilter
				.addFilterBefore(userValidationRateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}
}
