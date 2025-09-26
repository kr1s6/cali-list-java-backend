package com.CalisthenicList.CaliList.service;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.model.*;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import com.CalisthenicList.CaliList.utils.JwtUtils;
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
	private final JwtUtils jwtUtils;

	public ResponseEntity<UserAuthResponseDTO> registrationService(UserRegistrationDTO userDto) {
		UserAuthResponseDTO responseDTO = new UserAuthResponseDTO();
		//Validate user input
		Map<String, String> responseMessage = collectRegistrationErrors(userDto);
		if(!responseMessage.isEmpty()) {
			logger.warning("Registration failed due to errors.");
			responseDTO.setMessage(responseMessage);
			return new ResponseEntity<>(responseDTO, HttpStatus.CONFLICT);
		}
		//Encode password
		String rawPassword = userDto.getPassword();
		String encodedPassword = encoder.encode(rawPassword);
		userDto.setPassword(encodedPassword);
		if(encodedPassword.equals(rawPassword)) {
			responseMessage.put("service_error", Messages.SERVICE_ERROR);
			responseDTO.setMessage(responseMessage);
			logger.warning("Password encoding failed.");
			return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		//Save user to DB
		User user = Mapper.newUser(userDto);
		userRepository.save(user);
		//Send Async email verification
		emailService.postEmailVerificationToUser(user.getEmail());
		//Return response with jwt
		responseMessage.put("message", Messages.USER_REGISTERED_SUCCESS);
		responseDTO = createUserAuthResponseDTO(user, responseMessage);
		logger.info("User registered successfully.");
		return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
	}

	public ResponseEntity<UserAuthResponseDTO> loginService(UserLoginDTO userLoginDTO) {
		UserAuthResponseDTO responseDTO = new UserAuthResponseDTO();
		Map<String, String> responseMessage = new HashMap<>();
		//Validate if user exists
		Optional<User> userOptional = userRepository.findByEmail(userLoginDTO.getEmail());
		if(userOptional.isEmpty()) {
			logger.warning("Login attempt with non-existing email.");
			responseMessage.put("message", Messages.INVALID_LOGIN_ERROR);
			responseDTO.setMessage(responseMessage);
			return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
		}
		//Validate password
		User user = userOptional.get();
		boolean matches = encoder.matches(userLoginDTO.getPassword(), user.getPassword());
		if(!matches) {
			logger.warning("Invalid password for login attempt.");
			responseMessage.put("message", Messages.INVALID_LOGIN_ERROR);
			responseDTO.setMessage(responseMessage);
			return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
		}
		//Return response with jwt
		responseMessage.put("message", Messages.LOGIN_SUCCESS);
		responseDTO = createUserAuthResponseDTO(user, responseMessage);
		logger.info("User logged in successfully.");
		return new ResponseEntity<>(responseDTO, HttpStatus.OK);
	}

	public ResponseEntity<String> deleteUserById(UserDeleteByIdDTO userDeleteByIdDto) {
		UUID id = userDeleteByIdDto.getUserId();
		String password = userDeleteByIdDto.getPassword();
		//Validate if user exists
		Optional<User> user = userRepository.findById(id);
		if(user.isEmpty()) {
			logger.warning("Attempt to delete non-existing user.");
			return new ResponseEntity<>(Messages.SERVICE_ERROR, HttpStatus.NOT_FOUND);
		}
		//Validate if the password is valid for user id
		User userToDelete = user.get();
		if(!encoder.matches(password, userToDelete.getPassword())) {
			logger.warning("Invalid password for user deletion attempt.");
			return new ResponseEntity<>(Messages.SERVICE_ERROR, HttpStatus.UNAUTHORIZED);
		}
		//Delete user
		userRepository.delete(userToDelete);
		logger.info("User deleted successfully.");
		return new ResponseEntity<>("User deleted successfully", HttpStatus.OK);
	}

	private UserAuthResponseDTO createUserAuthResponseDTO(User user, Map<String, String> responseMessage) {
		String jwt = jwtUtils.generateJwtToken(user.getEmail());
		return new UserAuthResponseDTO(
				responseMessage,
				jwt,
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getRole().name(),
				user.isEmailVerified()
		);
	}

	private Map<String, String> collectRegistrationErrors(UserRegistrationDTO userDto) {
		Map<String, String> errors = new HashMap<>();
		String username = userDto.getUsername();
		String email = userDto.getEmail();
		String rawPassword = userDto.getPassword();
		String rawRepeatedPassword = userDto.getConfirmPassword();
		boolean emailAlreadyExists = userRepository.findByEmail(email).isPresent();
		boolean usernameAlreadyExists = userRepository.findByUsername(username).isPresent();
		boolean validRepeatablePassword = rawPassword.equals(rawRepeatedPassword);
		if(emailAlreadyExists) {
			errors.put("email", Messages.EMAIL_ALREADY_EXISTS_ERROR);
			logger.warning("Attempted registration with existing email.");
		} else {
			boolean emailDomainExists = emailService.dnsEmailLookup(email);
			if(!emailDomainExists) {
				errors.put("email", Messages.EMAIL_INVALID_ERROR);
				logger.warning("Attempted registration with invalid email domain.");
			}
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
}


