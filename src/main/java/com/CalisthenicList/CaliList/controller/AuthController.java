package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.model.UserAuthResponseDTO;
import com.CalisthenicList.CaliList.model.UserLoginDTO;
import com.CalisthenicList.CaliList.model.UserRegistrationDTO;
import com.CalisthenicList.CaliList.service.AuthService;
import com.CalisthenicList.CaliList.service.EmailService;
import com.CalisthenicList.CaliList.service.tokens.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {
	public static final String registerUrl = "/register";
	public static final String loginUrl = "/login";
	public static final String logoutUrl = "/logout";
	public static final String emailVerificationUrl = "/email-verification/{token}";
	public static final String refreshTokenUrl = "/refreshToken";
	private final AuthService authService;
	private final EmailService emailService;
	private final RefreshTokenService refreshTokenService;

	@PostMapping(registerUrl)
	//INFO registration request require Unique username, Unique email, password
	public ResponseEntity<UserAuthResponseDTO> registerUser(@Valid @RequestBody UserRegistrationDTO userDto,
															HttpServletResponse response) {
		return authService.registerUser(userDto, response);
//        TODO
//         - secure JWT token
//         - Implement Secure Password Recovery Mechanism
	}

	@PostMapping(loginUrl)
	//INFO login request require email and password
	public ResponseEntity<UserAuthResponseDTO> loginUser(@Valid @RequestBody UserLoginDTO userLoginDTO,
														 HttpServletResponse response) {
		return authService.loginUser(userLoginDTO, response);
//        TODO
//         - CAPTCHA
	}

	@PostMapping(logoutUrl)
	public ResponseEntity<?> logoutUser(@CookieValue(name = "refreshToken", required = false) String refreshToken,
										HttpServletResponse response) {
		return authService.logoutUser(refreshToken, response);
	}

	@GetMapping(emailVerificationUrl)
	public ResponseEntity<Map<String, String>> verifyEmail(@PathVariable("token") String token) {
		return emailService.verifyEmail(token);
//		TODO - w Gmailu/Google Workspace skonfigurujesz i zweryfikujesz alias „Send mail as”
//				(Wyślij jako) dla tego adresu, łącznie z poprawnym SPF/DKIM/DMARC dla domeny
	}

	@PostMapping(refreshTokenUrl)
	public ResponseEntity<?> refreshToken(HttpServletRequest request) {
		return refreshTokenService.getNewAccessToken(request);
	}


}
