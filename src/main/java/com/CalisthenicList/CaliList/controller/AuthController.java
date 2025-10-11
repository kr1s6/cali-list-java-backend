package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.model.ApiResponse;
import com.CalisthenicList.CaliList.model.UserLoginDTO;
import com.CalisthenicList.CaliList.model.UserRegistrationDTO;
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
//        TODO
//         - Implement Secure Password Recovery Mechanism
	}

	@PostMapping(loginUrl)
	//INFO login request require email and password
	public ResponseEntity<ApiResponse<Object>> loginUser(@Valid @RequestBody UserLoginDTO userLoginDTO,
														 HttpServletResponse response) {
		return authService.loginUser(userLoginDTO, response);
//        TODO
//         - CAPTCHA
	}

	@PostMapping(logoutUrl)
	public ResponseEntity<ApiResponse<Object>> logoutUser(@CookieValue(name = "refreshToken", required = false) String refreshToken,
														  HttpServletResponse response) {
		return authService.logoutUser(refreshToken, response);
	}

	@GetMapping(emailVerificationUrl)
	public ResponseEntity<ApiResponse<Object>> verifyEmail(@PathVariable("token") String token) {
		return emailService.verifyEmail(token);
//		TODO - w Gmailu/Google Workspace skonfigurujesz i zweryfikujesz alias „Send mail as”
//				(Wyślij jako) dla tego adresu, łącznie z poprawnym SPF/DKIM/DMARC dla domeny
	}

	@PostMapping(refreshTokenUrl)
	public ResponseEntity<?> refreshToken(@CookieValue(name = "refreshToken", required = false) String token,
										  HttpServletResponse response) {
		return refreshTokenService.refreshAccessToken(token, response);
	}
}