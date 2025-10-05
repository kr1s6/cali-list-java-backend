package com.CalisthenicList.CaliList.service;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.model.ApiResponse;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.model.UserDeleteByIdDTO;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class UserService {
	private final Logger logger = Logger.getLogger(UserService.class.getName());
	private final UserRepository userRepository;
	private final PasswordEncoder encoder;

	public ResponseEntity<ApiResponse<Object>> deleteUserById(UserDeleteByIdDTO userDeleteByIdDto) {
		UUID id = userDeleteByIdDto.getUserId();
		//Validate if user exists
		User user = userRepository.findById(id)
				.orElseThrow(() -> {
					logger.warning("Attempt to delete NON-existing user.");
					return new UsernameNotFoundException(Messages.SERVICE_ERROR);
				});

		//Validate if the password is valid for user id
		String password = userDeleteByIdDto.getPassword();
		if(!encoder.matches(password, user.getPassword())) {
			logger.warning("Invalid password for user deletion attempt.");
			throw new BadCredentialsException(Messages.SERVICE_ERROR);
		}
		//Delete user
		userRepository.delete(user);
		logger.info(Messages.USER_DELETED);
		return ResponseEntity.ok(
				ApiResponse.builder()
						.success(true)
						.message(Messages.USER_DELETED)
						.build()
		);
	}
}
