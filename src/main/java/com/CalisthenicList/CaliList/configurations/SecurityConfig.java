package com.CalisthenicList.CaliList.configurations;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

@Configuration
// INFO - Configuration should be used for @Bean definition
public class SecurityConfig {
	private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);
	private static final int saltLength = 16;
	private static final int hashLength = 32;
	private static final int parallelism = 1;
	private static final int memory = 12_288;
	private static final int iterations = 3;
	public static int TOKEN_CAPACITY = 5;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new Argon2PasswordEncoder(saltLength, hashLength, parallelism, memory, iterations);
	}

	@Bean
	public Bucket rateLimitBucket() {
		Bandwidth limit = Bandwidth.builder()
				.capacity(TOKEN_CAPACITY)
				.refillGreedy(TOKEN_CAPACITY, REFILL_PERIOD)
				.build();
		return Bucket.builder()
				.addLimit(limit)
				.build();
	}
}
