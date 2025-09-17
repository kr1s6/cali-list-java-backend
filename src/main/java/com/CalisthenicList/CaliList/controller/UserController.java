package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.model.UserLoginDTO;
import com.CalisthenicList.CaliList.model.UserRegistrationDTO;
import com.CalisthenicList.CaliList.service.EmailService;
import com.CalisthenicList.CaliList.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;
	private final EmailService emailService;

	@PostMapping("/register")
	//INFO registration request require Unique username, Unique email, password
	public ResponseEntity<List<String>> registerUser(@Valid @RequestBody UserRegistrationDTO userDto) {
		return userService.registrationService(userDto);
//        TODO
//         - Implement Secure Password Recovery Mechanism
	}

	@GetMapping("/email-verification/{token}")
	public ResponseEntity<String> verifyEmail(@PathVariable String token) {
		return emailService.verifyEmail(token);
//		TODO - w Gmailu/Google Workspace skonfigurujesz i zweryfikujesz alias „Send mail as”
//				(Wyślij jako) dla tego adresu, łącznie z poprawnym SPF/DKIM/DMARC dla domeny
	}

	@PostMapping("/login")
	public ResponseEntity<List<String>> loginUser(@Valid @RequestBody UserLoginDTO userLoginDTO) {
		return userService.loginService(userLoginDTO);
//        TODO
//         - User can use email or username as a [username]
//         - Compare Password Hashes Using Safe Functions
//         - "three strikes and you are out" policy is the pain for legitimate user
//         - user get new token every login attempt
	}

	@DeleteMapping("/delete/{id}")
	public ResponseEntity<String> deleteUserById(@PathVariable UUID id) {
		return userService.deleteUserById(id);
//      TODO
//       - need to be secured for admin, tests and for user to delete himself
	}
}






