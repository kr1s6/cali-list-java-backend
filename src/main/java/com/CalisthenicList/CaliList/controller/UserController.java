package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.model.UserDeleteByIdDTO;
import com.CalisthenicList.CaliList.model.UserLoginDTO;
import com.CalisthenicList.CaliList.model.UserRegistrationDTO;
import com.CalisthenicList.CaliList.model.UserAuthResponseDTO;
import com.CalisthenicList.CaliList.service.EmailService;
import com.CalisthenicList.CaliList.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {
	public static final String loginUrl = "/login";
	private final UserService userService;
	private final EmailService emailService;

	@PostMapping("/register")
	//INFO registration request require Unique username, Unique email, password
	public ResponseEntity<UserAuthResponseDTO> registerUser(@Valid @RequestBody UserRegistrationDTO userDto) {
		return userService.registrationService(userDto);
//        TODO
//         - secure JWT token
//         - Implement Secure Password Recovery Mechanism
	}

	@GetMapping("/email-verification/{token}")
	public ResponseEntity<Map<String, String>> verifyEmail(@PathVariable String token) {
		return emailService.verifyEmail(token);
//		TODO - w Gmailu/Google Workspace skonfigurujesz i zweryfikujesz alias „Send mail as”
//				(Wyślij jako) dla tego adresu, łącznie z poprawnym SPF/DKIM/DMARC dla domeny
	}

	@PostMapping(loginUrl)
	//INFO login request require email and password
	public ResponseEntity<UserAuthResponseDTO> loginUser(@Valid @RequestBody UserLoginDTO userLoginDTO) {
		return userService.loginService(userLoginDTO);
//        TODO
//         - CAPTCHA
	}

	@DeleteMapping("/delete/{id}")
	public ResponseEntity<String> deleteUserById(@Valid @RequestBody UserDeleteByIdDTO userDeleteByIdDto) {
		return userService.deleteUserById(userDeleteByIdDto);
//      TODO
//       - need to be secured for admin, tests and for user to delete himself
	}
}






