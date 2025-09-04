package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.model.UserLoginRequest;
import com.CalisthenicList.CaliList.model.UserRegistrationDTO;
import com.CalisthenicList.CaliList.service.UserControllerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
	private final UserControllerService userControllerService;

	@PostMapping("/register")
	//INFO registration request require Unique username, Unique email, password
	public ResponseEntity<List<String>> registerUser(@Valid @RequestBody UserRegistrationDTO userDto) {
		return userControllerService.registrationService(userDto);
//        TODO
//         - check if you can do user with Amin role by Postman
//         - Include a password strength meter (zxcvbn-ts library) (Frontend)
//         - Include confirm password tile only on frontend side for user-friendly authentication (Frontend)
//         - check if this endpoint need to return user object or you get it differently
//         - Implement Secure Password Recovery Mechanism
//         - Implement validation for emails
	}

	@PostMapping("/login")
	public ResponseEntity<List<String>> loginUser(@Valid @RequestBody UserLoginRequest userLoginRequest) {
		return userControllerService.loginService(userLoginRequest);
//        TODO
//         - User can use email or username as a [username]
//         - Compare Password Hashes Using Safe Functions
//         - "three strikes and you are out" policy is the pain for legitimate user
//         - user get new token every login attempt
	}

	@DeleteMapping("/delete/{id}")
	public ResponseEntity<String> deleteUserById(@PathVariable Long id) {
		return userControllerService.deleteUserById(id);
//      TODO
//       - need to be secured for admin, tests and for user to delete himself
	}
}






