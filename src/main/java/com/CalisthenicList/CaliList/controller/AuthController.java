package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.model.*;
import com.CalisthenicList.CaliList.service.AuthService;
import com.CalisthenicList.CaliList.service.EmailService;
import com.CalisthenicList.CaliList.service.tokens.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {
	public static final String registerUrl = "/api/register";
	public static final String loginUrl = "/api/login";
	public static final String sendPasswordRecoveryRequestUrl = "/api/password-recovery";
	public static final String passwordRecoveryUrl = "/api/password-recovery/{token}";
	public static final String logoutUrl = "/api/logout";
	public static final String emailVerificationUrl = "/api/email-verification/{token}";
	public static final String refreshTokenUrl = "/api/refreshToken";
	private final AuthService authService;
	private final EmailService emailService;
	private final RefreshTokenService refreshTokenService;

	@PostMapping(registerUrl)
	//INFO registration request require Unique username, Unique email, password
	public ResponseEntity<ApiResponse<Object>> registerUser(@Valid @RequestBody UserRegistrationDTO userDto,
															HttpServletResponse response) {
		return authService.registerUser(userDto, response);
	}

	@PostMapping(sendPasswordRecoveryRequestUrl)
	//INFO send on email the url to recover password
	public ResponseEntity<ApiResponse<Object>> sendRecoverPasswordEmail(@Valid @RequestBody EmailDTO emailDTO) {
		return authService.sendPasswordRecoveryEmail(emailDTO);
	}

	@PostMapping(passwordRecoveryUrl)
	//INFO set new password for user by using token with email from url and DTO with passwords
	public ResponseEntity<ApiResponse<Object>> passwordRecovery(@PathVariable("token") String jwt, @Valid @RequestBody PasswordRecoveryDTO passwordRecoveryDTO) {
		return authService.passwordRecovery(jwt, passwordRecoveryDTO);
	}

	@PostMapping(loginUrl)
	//INFO login request require email and password
	public ResponseEntity<ApiResponse<Object>> loginUser(@Valid @RequestBody UserLoginDTO userLoginDTO,
														 HttpServletResponse response) {
		return authService.loginUser(userLoginDTO, response);
	}

	@PostMapping(logoutUrl)
	public ResponseEntity<ApiResponse<Object>> logoutUser(@CookieValue(name = "refreshToken", required = false) String refreshToken,
														  HttpServletResponse response) {
		return authService.logoutUser(refreshToken, response);
	}

	@GetMapping(emailVerificationUrl)
	public ResponseEntity<ApiResponse<Object>> verifyEmail(@PathVariable("token") String jwt) {
		return emailService.verifyEmail(jwt);
//		TODO - w Gmailu/Google Workspace skonfigurujesz i zweryfikujesz alias „Send mail as”
//				(Wyślij jako) dla tego adresu, łącznie z poprawnym SPF/DKIM/DMARC dla domeny
//		INFO - Maybe it's not needed for this app
	}

	@PostMapping(refreshTokenUrl)
	public ResponseEntity<?> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken,
										  HttpServletResponse response) {
		return refreshTokenService.refreshAccessToken(refreshToken, response);
	}
}