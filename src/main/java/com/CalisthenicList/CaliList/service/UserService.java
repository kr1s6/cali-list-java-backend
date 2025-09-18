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

import java.util.ArrayList;
import java.util.List;
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

	public ResponseEntity<List<String>> registrationService(UserRegistrationDTO userDto) {
		String rawPassword = userDto.getPassword();
		List<String> responseBody = collectRegistrationErrors(userDto);
		if(!responseBody.isEmpty()) {
			logger.warning("Registration failed due to errors.");
			return new ResponseEntity<>(responseBody, HttpStatus.CONFLICT);
		}
		String encodedPassword = encoder.encode(rawPassword);
		userDto.setPassword(encodedPassword);
		if(encodedPassword.equals(rawPassword)) {
			responseBody.add(Messages.SERVICE_ERROR);
			logger.warning("Password encoding failed.");
			return new ResponseEntity<>(responseBody, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		User user = Mapper.newUser(userDto);
		userRepository.save(user);
		emailService.postEmailVerificationToUser(user.getId(), user.getEmail());
		responseBody.add(Messages.USER_REGISTERED_SUCCESS);
		logger.info("User registered successfully.");
		return new ResponseEntity<>(responseBody, HttpStatus.CREATED);
	}

	private List<String> collectRegistrationErrors(UserRegistrationDTO userDto) {
		List<String> errors = new ArrayList<>();
		String username = userDto.getUsername();
		String email = userDto.getEmail();
		String rawPassword = userDto.getPassword();
		String rawRepeatedPassword = userDto.getConfirmPassword();
		boolean emailAlreadyExists = userRepository.findByEmail(email).isPresent();
		boolean emailDomainExists = emailService.dnsEmailLookup(email);
		boolean usernameAlreadyExists = userRepository.findByUsername(username).isPresent();
		boolean validRepeatablePassword = rawPassword.equals(rawRepeatedPassword);
		if(emailAlreadyExists) {
			errors.add(Messages.EMAIL_ALREADY_EXISTS_ERROR);
			logger.warning("Attempted registration with existing email.");
		}
		if(!emailDomainExists) {
			errors.add(Messages.EMAIL_INVALID_ERROR);
			logger.warning("Attempted registration with invalid email domain.");
		}
		if(usernameAlreadyExists) {
			errors.add(Messages.USERNAME_ALREADY_EXISTS_ERROR);
			logger.warning("Attempted registration with existing username.");
		}
		if(!validRepeatablePassword) {
			errors.add(Messages.INVALID_CONFIRM_PASSWORD_ERROR);
			logger.warning("Wrong password confirmation.");
		}
		return errors;
	}

	public ResponseEntity<List<String>> loginService(UserLoginDTO userLoginDTO) {
		List<String> responseBody = new ArrayList<>();
		Optional<User> userOptional = userRepository.findByEmail(userLoginDTO.getUsername());
		if(userOptional.isEmpty()) {
			logger.warning("Login attempt with non-existing email.");
			responseBody.add(Messages.PASSWORD_INVALID_LOGIN_ERROR);
			return new ResponseEntity<>(responseBody, HttpStatus.UNAUTHORIZED);
		}

		User user = userOptional.get();
		boolean matches = encoder.matches(userLoginDTO.getPassword(), user.getPassword());
		if(!matches) {
			logger.warning("Invalid password attempt for email.");
			responseBody.add(Messages.PASSWORD_INVALID_LOGIN_ERROR);
			return new ResponseEntity<>(responseBody, HttpStatus.UNAUTHORIZED);
		}
		logger.info("User logged in successfully.");
		responseBody.add(Messages.LOGIN_SUCCESS);
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










