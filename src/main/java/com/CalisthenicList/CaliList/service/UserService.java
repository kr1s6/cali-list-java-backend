package com.CalisthenicList.CaliList.service;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.model.UserLoginDTO;
import com.CalisthenicList.CaliList.model.UserRegistrationDTO;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import com.CalisthenicList.CaliList.utils.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class UserService {
	private final Logger logger = Logger.getLogger(UserService.class.getName());
	private final UserRepository userRepository;
	private final PasswordEncoder encoder;
	private final EmailService emailService;
	private final JwtService jwtService;

	public ResponseEntity<Map<String, String>> registrationService(UserRegistrationDTO userDto) {
		String rawPassword = userDto.getPassword();
		Map<String, String> responseBody = collectRegistrationErrors(userDto);
		if(!responseBody.isEmpty()) {
			logger.warning("Registration failed due to errors.");
			return new ResponseEntity<>(responseBody, HttpStatus.CONFLICT);
		}
		String encodedPassword = encoder.encode(rawPassword);
		userDto.setPassword(encodedPassword);
		if(encodedPassword.equals(rawPassword)) {
			responseBody.put("service_error", Messages.SERVICE_ERROR);
			logger.warning("Password encoding failed.");
			return new ResponseEntity<>(responseBody, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		User user = Mapper.newUser(userDto);
		userRepository.save(user);
		emailService.postEmailVerificationToUser(user.getId(), user.getEmail());
		responseBody.put("message", Messages.USER_REGISTERED_SUCCESS);
		String jwt = jwtService.generateJwtToken(user.getEmail());
		responseBody.put("jwt", jwt);
		logger.info("User registered successfully.");
		return new ResponseEntity<>(responseBody, HttpStatus.CREATED);
	}

	private Map<String, String> collectRegistrationErrors(UserRegistrationDTO userDto) {
		Map<String, String> errors = new HashMap<>();
		String username = userDto.getUsername();
		String email = userDto.getEmail();
		String rawPassword = userDto.getPassword();
		String rawRepeatedPassword = userDto.getConfirmPassword();
		boolean emailAlreadyExists = userRepository.findByEmail(email).isPresent();
		boolean emailDomainExists = emailService.dnsEmailLookup(email);
		boolean usernameAlreadyExists = userRepository.findByUsername(username).isPresent();
		boolean validRepeatablePassword = rawPassword.equals(rawRepeatedPassword);
		if(emailAlreadyExists) {
			errors.put("email", Messages.EMAIL_ALREADY_EXISTS_ERROR);
			logger.warning("Attempted registration with existing email.");
		}
		if(!emailDomainExists) {
			errors.put("email", Messages.EMAIL_INVALID_ERROR);
			logger.warning("Attempted registration with invalid email domain.");
		}
		if(usernameAlreadyExists) {
			errors.put("username", Messages.USERNAME_ALREADY_EXISTS_ERROR);
			logger.warning("Attempted registration with existing username.");
		}
		if(!validRepeatablePassword) {
			errors.put("password", Messages.INVALID_CONFIRM_PASSWORD_ERROR);
			logger.warning("Wrong password confirmation.");
		}
		return errors;
	}

	public ResponseEntity<Map<String, String>> loginService(UserLoginDTO userLoginDTO) {
		Map<String, String> responseBody = new HashMap<>();
		Optional<User> userOptional = userRepository.findByEmail(userLoginDTO.getEmail());
		if(userOptional.isEmpty()) {
			logger.warning("Login attempt with non-existing email.");
			responseBody.put("message", Messages.INVALID_LOGIN_ERROR);
			return new ResponseEntity<>(responseBody, HttpStatus.UNAUTHORIZED);
		}
		User user = userOptional.get();
		boolean matches = encoder.matches(userLoginDTO.getPassword(), user.getPassword());
		if(!matches) {
			logger.warning("Invalid password attempt for email.");
			responseBody.put("message", Messages.INVALID_LOGIN_ERROR);
			return new ResponseEntity<>(responseBody, HttpStatus.UNAUTHORIZED);
		}
		logger.info("User logged in successfully.");
		String jwt = jwtService.generateJwtToken(user.getEmail());
		responseBody.put("message", Messages.LOGIN_SUCCESS);
		responseBody.put("jwt", jwt);
		return new ResponseEntity<>(responseBody, HttpStatus.OK);
	}

	public ResponseEntity<String> deleteUserById(UUID id) {
		Optional<User> userOptional = userRepository.findById(id);
		if(userOptional.isEmpty()) {
			logger.warning("Attempt to delete non-existing user.");
			return new ResponseEntity<>(Messages.SERVICE_ERROR, HttpStatus.NOT_FOUND);
		}
		User userToDelete = userOptional.get();
		userRepository.delete(userToDelete);
		logger.info("User deleted successfully.");
		return new ResponseEntity<>("User deleted successfully", HttpStatus.OK);
	}
}


