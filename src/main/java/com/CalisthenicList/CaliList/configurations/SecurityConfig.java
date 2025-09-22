package com.CalisthenicList.CaliList.configurations;

import com.CalisthenicList.CaliList.filter.JwtAuthenticationFilter;
import com.CalisthenicList.CaliList.filter.UserValidationRateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
// INFO - Configuration should be used for @Bean definition
public class SecurityConfig {
	private static final int saltLength = 16;
	private static final int hashLength = 32;
	private static final int parallelism = 1;
	private static final int memory = 12_288;
	private static final int iterations = 3;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final UserValidationRateLimitingFilter userValidationRateLimitingFilter;
	private final AuthenticationEntryPointJwt authenticationEntryPointJwt;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new Argon2PasswordEncoder(saltLength, hashLength, parallelism, memory, iterations);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				//Not needed with REST API and JWT
				.csrf(AbstractHttpConfigurer::disable)
				//The front-end side needs error, not redirection
				.exceptionHandling(exceptionHandling ->
						exceptionHandling.authenticationEntryPoint(authenticationEntryPointJwt))
				//Not use sessions with JWT token and REST API
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(requests -> requests
						.requestMatchers("/delete/**").authenticated()
						.anyRequest().permitAll()
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(userValidationRateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}
}
