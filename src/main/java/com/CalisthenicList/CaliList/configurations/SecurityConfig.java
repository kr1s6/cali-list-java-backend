package com.CalisthenicList.CaliList.configurations;

import com.CalisthenicList.CaliList.filter.JwtAuthFilter;
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
	private final JwtAuthFilter jwtAuthFilter;
	private final UserValidationRateLimitingFilter userValidationRateLimitingFilter;
	private final AuthEntryPointJwt authEntryPointJwt;

	@Bean
	public PasswordEncoder passwordEncoder() {
		int saltLength = 16;
		int hashLength = 32;
		int parallelism = 1;
		int memory = 12_288;
		int iterations = 3;
		return new Argon2PasswordEncoder(saltLength, hashLength, parallelism, memory, iterations);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				//Not needed with REST API and JWT
				.csrf(AbstractHttpConfigurer::disable)
				//The front-end side needs error, not redirection
				.exceptionHandling(exceptionHandling ->
						exceptionHandling.authenticationEntryPoint(authEntryPointJwt))
				//Not use sessions with JWT token and REST API
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(requests -> requests
						.requestMatchers("/delete/**").authenticated()
						.anyRequest().permitAll()
				)
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(userValidationRateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}
}
