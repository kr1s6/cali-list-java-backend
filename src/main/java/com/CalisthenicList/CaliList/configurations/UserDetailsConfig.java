package com.CalisthenicList.CaliList.configurations;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
@RequiredArgsConstructor
public class UserDetailsConfig {

	private final UserRepository userRepository;

	@Bean
	public UserDetailsService userDetailsService() {
		return username -> userRepository.findByEmail(username)
				.orElseThrow(() -> new UsernameNotFoundException(Messages.USER_NOT_FOUND));
	}
}
