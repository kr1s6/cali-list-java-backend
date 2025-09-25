package com.CalisthenicList.CaliList.service;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.constants.UserConstants;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.model.UserLoginDTO;
import com.CalisthenicList.CaliList.model.UserRegistrationDTO;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private EmailService emailService;
	@Mock
	private JwtService jwtService;
	@InjectMocks
	private UserService userService;

	@Nested
	@DisplayName("registrationService")
	class RegistrationServiceTest {

		private final String password = "qWBRęLGć8MPł_test";
		private UserRegistrationDTO userRegistrationDTO;

		@BeforeEach
		void initEach() {
			String username = "TestUser";
			String email = "test@intera.pl";
			userRegistrationDTO = new UserRegistrationDTO(username, email, password, password);
			Mockito.when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
			Mockito.when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
			Mockito.when(emailService.dnsEmailLookup(anyString())).thenReturn(true);
		}

		@Test
		@DisplayName("✅ Happy Case: User registered with valid credentials")
		void givenValidUserDTO_whenRegister_thenReturnUserRegisteredSuccess() {
			// Given
			Mockito.when(jwtService.generateJwtToken(anyString())).thenReturn("sdasfsfsfsfsfsf");
			Mockito.when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
			// When
			ResponseEntity<Map<String, String>> response = userService.registrationService(userRegistrationDTO);
			// Then
			System.out.println(response);
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.CREATED), "Registration failed. Code: " + response.getStatusCode());
			Assertions.assertNotNull(response.getBody());
			assertEquals(Messages.USER_REGISTERED_SUCCESS, response.getBody().get("message"), "Wrong error message.");
		}

		@Test
		@DisplayName("❌ Negative Case: Invalid email dns")
		void givenInvalidEmailDns_whenRegister_thenReturnEmailInvalidError() {
			// Given
			Mockito.when(emailService.dnsEmailLookup(anyString())).thenReturn(false);
			// When
			ResponseEntity<Map<String, String>> response = userService.registrationService(userRegistrationDTO);
			// Then
			System.out.println(response);
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT), "Should return Conflict.");
			Assertions.assertNotNull(response.getBody());
			assertEquals(Messages.EMAIL_INVALID_ERROR, response.getBody().get("message"), "Wrong error message.");
		}

		@Test
		@DisplayName("❌ Negative Case: Already registered email")
		void givenExistingEmail_whenRegister_thenReturnEmailAlreadyExistError() {
			// Given
			Mockito.when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User("test", "test", "test")));
			// When
			ResponseEntity<Map<String, String>> response = userService.registrationService(userRegistrationDTO);
			// Then
			System.out.println(response);
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT), "Should return Conflict.");
			Assertions.assertNotNull(response.getBody());
			assertEquals(Messages.EMAIL_ALREADY_EXISTS_ERROR, response.getBody().get("message"), "Wrong error message.");
		}

		@Test
		@DisplayName("❌ Negative Case: Already existing username")
		void givenExistingUsername_whenRegister_thenReturnUsernameAlreadyExistError() {
			// Given
			Mockito.when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(new User("test", "test", "test")));
			// When
			ResponseEntity<Map<String, String>> response = userService.registrationService(userRegistrationDTO);
			// Then
			System.out.println(response);
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT), "Should return Conflict.");
			Assertions.assertNotNull(response.getBody());
			assertEquals(Messages.USERNAME_ALREADY_EXISTS_ERROR, response.getBody().get("message"), "Wrong error message.");
		}

		@Test
		@DisplayName("❌ Negative Case: Wrong confirm password")
		void givenWrongConfirmPassword_whenRegister_thenReturnInvalidConfirmPasswordError() {
			// Given
			userRegistrationDTO.setConfirmPassword("test");
			// When
			ResponseEntity<Map<String, String>> response = userService.registrationService(userRegistrationDTO);
			// Then
			System.out.println(response);
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT), "Should return Conflict.");
			Assertions.assertNotNull(response.getBody());
			assertEquals(Messages.INVALID_CONFIRM_PASSWORD_ERROR, response.getBody().get("message"), "Wrong error message.");
		}

		@Test
		@DisplayName("❌ Negative Case: Wrong confirm password")
		void givenTooLongPassword_whenRegister_thenReturnInvalidConfirmPasswordError() {
			// Given
			userRegistrationDTO.setPassword("a".repeat(UserConstants.PASSWORD_MAX_LENGTH + 1));
			userRegistrationDTO.setConfirmPassword("a".repeat(UserConstants.PASSWORD_MAX_LENGTH + 1));
			// When
			ResponseEntity<Map<String, String>> response = userService.registrationService(userRegistrationDTO);
			// Then
			System.out.println(response);
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT), "Should return Conflict.");
			Assertions.assertNotNull(response.getBody());
			assertEquals(Messages.INVALID_CONFIRM_PASSWORD_ERROR, response.getBody().get("message"), "Wrong error message.");
		}

		@Test
		@DisplayName("❌ Negative Case: Invalid password encoder")
		void givenWrongPasswordEncoder_whenRegister_thenReturnServiceError() {
			// Given
			Mockito.when(passwordEncoder.encode(password)).thenReturn(password);
			// When
			ResponseEntity<Map<String, String>> response = userService.registrationService(userRegistrationDTO);
			// Then
			System.out.println(response);
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.INTERNAL_SERVER_ERROR), "Should return Internal Server Error.");
			Assertions.assertNotNull(response.getBody());
			assertEquals(Messages.SERVICE_ERROR, response.getBody().get("message"), "Wrong error message.");
		}

		@Test
		@DisplayName("❌ Negative Case: Multiple errors")
		void givenAllWrong_whenRegister_thenReturnMultipleErrors() {
			// Given
			Mockito.when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User("test", "test", "test")));
			Mockito.when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(new User("test", "test", "test")));
			Mockito.when(emailService.dnsEmailLookup(anyString())).thenReturn(false);
			userRegistrationDTO.setConfirmPassword("test");
			// When
			ResponseEntity<Map<String, String>> response = userService.registrationService(userRegistrationDTO);
			// Then
			System.out.println(response);
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT), "Should return Conflict.");
			Map<String, String> expectedErrors = Map.of(
					"email", Messages.EMAIL_INVALID_ERROR,
					"password", Messages.INVALID_CONFIRM_PASSWORD_ERROR,
					"username", Messages.USERNAME_ALREADY_EXISTS_ERROR
			);
			Assertions.assertNotNull(response.getBody());
			assertEquals(expectedErrors, response.getBody(), "Wrong error message.");
		}

	}

	@Nested
	@DisplayName("loginService")
	class LoginServiceTest {

		private final String password = "qWBRęLGć8MPł_test";
		private UserLoginDTO userLoginDTO;

		@BeforeEach
		void initEach() {
			String email = "test@intera.pl";
			userLoginDTO = new UserLoginDTO(email, password);
			Mockito.when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
			Mockito.when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
			Mockito.when(emailService.dnsEmailLookup(anyString())).thenReturn(true);
		}

		//@Test
		//@DisplayName("✅ Happy Case: User registered with valid credentials")
		//void givenValidUserDTO_whenRegister_thenReturnUserRegisteredSuccess() {
		//	// Given
		//	Mockito.when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
		//	// When
		//	ResponseEntity<Map<String, String>> response = userService.registrationService(userLoginDTO);
		//	// Then
		//	System.out.println(response);
		//	assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.CREATED), "Registration failed. Code: " + response.getStatusCode());
		//	assertEquals(response.getBody(), List.of(Messages.USER_REGISTERED_SUCCESS), "Wrong error message.");
		//}
		//
		//@Test
		//@DisplayName("❌ Negative Case: Invalid email dns")
		//void givenInvalidEmailDns_whenRegister_thenReturnEmailInvalidError() {
		//	// Given
		//	Mockito.when(emailService.dnsEmailLookup(anyString())).thenReturn(false);
		//	// When
		//	ResponseEntity<Map<String, String>> response = userService.registrationService(userLoginDTO);
		//	// Then
		//	System.out.println(response);
		//	assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT), "Should return Conflict.");
		//	assertEquals(response.getBody(), List.of(Messages.EMAIL_INVALID_ERROR), "Wrong error message.");
		//}


	}

	@Nested
	@DisplayName("deleteUserById")
	class DeleteUserByIdTest {
		private String deleteUserUrl;

//				@BeforeEach
//				void initAll() {
//					deleteUserUrl = "http://localhost:" + port + "/delete/";
//					user = new User();
//				}
		//		TODO DO zrobienia testy
		//		testRestTemplate.delete(deleteUserUrl + createdUser.getId());
	}
}
