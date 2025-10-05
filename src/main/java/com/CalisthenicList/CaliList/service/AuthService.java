package com.CalisthenicList.CaliList.service;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.exceptions.UserRegistrationException;
import com.CalisthenicList.CaliList.model.*;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import com.CalisthenicList.CaliList.service.tokens.AccessTokenService;
import com.CalisthenicList.CaliList.service.tokens.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final Logger logger = Logger.getLogger(AuthService.class.getName());
	private final UserRepository userRepository;
	private final PasswordEncoder encoder;
	private final EmailService emailService;
	private final RefreshTokenService refreshTokenService;
	private final AccessTokenService accessTokenService;

	public ResponseEntity<ApiResponse<Object>> registerUser(UserRegistrationDTO userDto, HttpServletResponse response) {
		//Validate user input
		handleRegistrationErrors(userDto);
		//Encode password
		String rawPassword = userDto.getPassword();
		String encodedPassword = encoder.encode(rawPassword);
		if(encodedPassword.equals(rawPassword)) {
			logger.severe("Password encoding failed.");
			throw new RuntimeException("Password encoding failed.");
		}

		//Save user to DB
		User user = new User(userDto.getUsername(), userDto.getEmail(), encodedPassword);
		userRepository.save(user);

		//Create cookie with refresh token
		String userEmail = user.getEmail();
		ResponseCookie cookieWithRefreshToken = refreshTokenService.createCookieWithRefreshToken(userEmail);
		response.addHeader(HttpHeaders.SET_COOKIE, cookieWithRefreshToken.toString());

		//Create an access token
		String accessToken = accessTokenService.generateAccessToken(userEmail);

		//Send Async email verification
		emailService.postEmailVerificationToUser(userEmail);

		//Return response with userDTO and access token
		logger.info(Messages.USER_REGISTERED_SUCCESS);
		UserDTO userDTO = new UserDTO(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getRole(),
				user.isEmailVerified(),
				user.getBirthDate()
		);
		return ResponseEntity.status(HttpStatus.CREATED).body(
				ApiResponse.builder()
						.success(true)
						.message(Messages.USER_REGISTERED_SUCCESS)
						.data(userDTO)
						.accessToken(accessToken)
						.build()
		);
	}

	public ResponseEntity<ApiResponse<Object>> loginUser(UserLoginDTO userLoginDTO, HttpServletResponse response) {
		//Validate if user exists
		User user = userRepository.findByEmail(userLoginDTO.getEmail())
				.orElseThrow(() -> {
					logger.warning("Login attempt with non-existing email.");
					return new UsernameNotFoundException(Messages.INVALID_LOGIN_ERROR);
				});
		//Validate password
		boolean matches = encoder.matches(userLoginDTO.getPassword(), user.getPassword());
		if(!matches) {
			logger.warning("Invalid password for login attempt.");
			throw new BadCredentialsException(Messages.INVALID_LOGIN_ERROR);
		}

		//Create cookie with refresh token
		String userEmail = user.getEmail();
		ResponseCookie cookieWithRefreshToken = refreshTokenService.createCookieWithRefreshToken(userEmail, user);
		response.addHeader(HttpHeaders.SET_COOKIE, cookieWithRefreshToken.toString());

		//Create an access token
		String accessToken = accessTokenService.generateAccessToken(userEmail);

		//Return userDTO with an access token
		logger.info(Messages.LOGIN_SUCCESS);
		UserDTO userDTO = new UserDTO(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getRole(),
				user.isEmailVerified(),
				user.getBirthDate()
		);
		return ResponseEntity.ok(
				ApiResponse.builder()
						.success(true)
						.message(Messages.LOGIN_SUCCESS)
						.data(userDTO)
						.accessToken(accessToken)
						.build()
		);
	}

	public ResponseEntity<ApiResponse<Object>> logoutUser(String refreshToken, HttpServletResponse response) {
		return refreshTokenService.deleteRefreshToken(refreshToken, response);
	}

	private void handleRegistrationErrors(UserRegistrationDTO userDto) {
		Map<String, String> errors = new HashMap<>();
		String username = userDto.getUsername();
		String email = userDto.getEmail();
		String rawPassword = userDto.getPassword();
		String rawRepeatedPassword = userDto.getConfirmPassword();
		boolean emailAlreadyExists = userRepository.existsByEmail(email);
		boolean usernameAlreadyExists = userRepository.existsByUsername(username);
		boolean validRepeatablePassword = rawPassword.equals(rawRepeatedPassword);

		if(emailAlreadyExists) {
			errors.put("email", Messages.EMAIL_ALREADY_EXISTS_ERROR);
		} else {
			boolean emailDomainExists = emailService.dnsEmailLookup(email);
			if(!emailDomainExists) {
				errors.put("email", Messages.EMAIL_INVALID_ERROR);
				logger.warning("Attempted registration with invalid email domain.");
			}
		}
		if(usernameAlreadyExists) {
			errors.put("username", Messages.USERNAME_ALREADY_EXISTS_ERROR);
		}
		if(!validRepeatablePassword) {
			errors.put("password", Messages.INVALID_CONFIRM_PASSWORD_ERROR);
		}

		if(!errors.isEmpty()) {
			logger.warning(Messages.USER_REGISTERED_FAILED);
			throw new UserRegistrationException(errors);
		}
	}
}