package com.CalisthenicList.CaliList.service;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.model.UserLoginRequest;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class UserControllerService {
	private static final Logger logger = Logger.getLogger(UserControllerService.class.getName());
	private final UserRepository userRepository;
	private final Argon2PasswordEncoder encoder;
	private final EmailService emailService;

	public ResponseEntity<List<String>> registrationService(User user) {
		String username = user.getUsername();
		String email = user.getEmail();
		String notHashedPassword = user.getPassword();
		boolean emailAlreadyExists = userRepository.findByEmail(email).isPresent();
		boolean usernameAlreadyExists = userRepository.findByUsername(username).isPresent();
		boolean emailDomainExists = emailService.dnsEmailLookup(user.getEmail());
		List<String> responseBody = new ArrayList<>();

		if(emailAlreadyExists) {
			responseBody.add(Messages.EMAIL_ALREADY_EXISTS_ERROR);
			logger.warning("Attempted registration with existing email.");
		}
		if(usernameAlreadyExists) {
			responseBody.add(Messages.USERNAME_ALREADY_EXISTS_ERROR);
			logger.warning("Attempted registration with existing username.");
		}
		if(!emailDomainExists) {
			responseBody.add(Messages.EMAIL_INVALID_ERROR);
			logger.warning("Attempted registration with invalid email domain.");
		}
		if(emailAlreadyExists || usernameAlreadyExists || !emailDomainExists) {
			return new ResponseEntity<>(responseBody, HttpStatus.CONFLICT);
		}

		user.setPassword(encoder.encode(notHashedPassword));
		if(user.getPassword().equals(notHashedPassword)) {
			responseBody.add(Messages.SERVICE_ERROR);
			logger.warning("Password encoding failed.");
			return new ResponseEntity<>(responseBody, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		userRepository.save(user);
		responseBody.add(Messages.USER_REGISTERED_SUCCESS);
		return new ResponseEntity<>(responseBody, HttpStatus.CREATED);
	}

	public ResponseEntity<List<String>> loginService(UserLoginRequest userLoginRequest) {
		List<String> responseBody = new ArrayList<>();
		Optional<User> userOptional = userRepository.findByEmail(userLoginRequest.getUsername());
		if(userOptional.isEmpty()) {
			logger.warning("Login attempt with non-existing email.");
			responseBody.add(Messages.PASSWORD_INVALID_LOGIN_ERROR);
			return new ResponseEntity<>(responseBody, HttpStatus.UNAUTHORIZED);
		}

		User user = userOptional.get();
		boolean matches = encoder.matches(userLoginRequest.getPassword(), user.getPassword());
		if(!matches) {
			logger.warning("Invalid password attempt for email.");
			responseBody.add(Messages.PASSWORD_INVALID_LOGIN_ERROR);
			return new ResponseEntity<>(responseBody, HttpStatus.UNAUTHORIZED);
		}
		logger.info("User logged in successfully.");
		responseBody.add(Messages.LOGIN_SUCCESS);
		return new ResponseEntity<>(responseBody, HttpStatus.OK);
	}

	public ResponseEntity<String> deleteUserService(String email) {
		Optional<User> userOptional = userRepository.findByEmail(email);
		if(userOptional.isEmpty()) {
			logger.warning("Attempt to delete non-existing user.");
			return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
		}
		User userToDelete = userOptional.get();
		userRepository.delete(userToDelete);
		return new ResponseEntity<>("User " + email + " deleted successfully", HttpStatus.OK);
	}
}










