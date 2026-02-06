package com.CalisthenicList.CaliList.configurations.security;

import com.CalisthenicList.CaliList.filter.AccessTokenAuthFilter;
import com.CalisthenicList.CaliList.filter.UserValidationRateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
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
	private final AccessTokenAuthFilter accessTokenAuthFilter;
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

	/**
	 * csrf disabled - REST API don't use csrf, because it doesn't use session and cookies.
	 * cors - will use addCorsMappings from {@code WebConfig}.
	 * custom exceptionHandling - The front-end side needs error, not redirection. Use {@code AuthEntryPointJwt}.
	 * stateless session - Not use sessions with JWT token and REST API.
	 * @see <a href="https://www.baeldung.com/spring-security-sign-jwt-tokne">Info reference</a>
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPointJwt))
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(requests -> requests
						.requestMatchers("/api/delete/**").authenticated()
						.anyRequest().permitAll()
				)
				.addFilterBefore(accessTokenAuthFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(userValidationRateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}
}
