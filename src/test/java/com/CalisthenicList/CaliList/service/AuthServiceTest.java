package com.CalisthenicList.CaliList.service;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.exceptions.UserRegistrationException;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.model.UserAuthResponseDTO;
import com.CalisthenicList.CaliList.model.UserLoginDTO;
import com.CalisthenicList.CaliList.model.UserRegistrationDTO;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import com.CalisthenicList.CaliList.service.tokens.AccessTokenService;
import com.CalisthenicList.CaliList.service.tokens.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private EmailService emailService;
	@Mock
	private RefreshTokenService refreshTokenService;
	@Mock
	private AccessTokenService accessTokenService;
	@InjectMocks
	private AuthService authService;
	private MockHttpServletResponse mockResponse;

	private boolean dnsEmailLookup(String email) {
		return emailService.dnsEmailLookup(email);
	}

	private Optional<User> findByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	private String generateAccessToken(String email){
		return accessTokenService.generateAccessToken(email);
	}

	@Nested
	@DisplayName("registerUser")
	class RegisterUserServiceTest {
		private final String password = "qWBRęLGć8MPł_test";
		private UserRegistrationDTO userRegistrationDTO;

		private ResponseEntity<UserAuthResponseDTO> registerUser(UserRegistrationDTO userRegistrationDTO, MockHttpServletResponse mockResponse) {
			return authService.registerUser(userRegistrationDTO, mockResponse);
		}

		private boolean existsByEmail(String email) {
			return userRepository.existsByEmail(email);
		}

		private boolean existsByUsername(String username) {
			return userRepository.existsByUsername(username);
		}

		@BeforeEach
		void initEach() {
			String username = "TestUser";
			String email = "test@intera.pl";
			userRegistrationDTO = new UserRegistrationDTO(username, email, password, password);
			mockResponse = new MockHttpServletResponse();
		}

		@Test
		@DisplayName("✅ Happy Case: User registered with valid credentials")
		void givenValidUserDTO_whenRegister_thenReturnUserRegisteredSuccess() {
			// Given
			String fakeRefreshToken = "refresh-token-123";
			String fakeAccessToken = "access-token-123";
			Mockito.when(dnsEmailLookup(anyString())).thenReturn(true);
			Mockito.when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
			Mockito.when(refreshTokenService.createCookieWithRefreshToken(anyString()))
					.thenReturn(ResponseCookie.from("refreshToken", fakeRefreshToken).build());
			Mockito.when(generateAccessToken(anyString())).thenReturn(fakeAccessToken);
			// When
			ResponseEntity<UserAuthResponseDTO> response = registerUser(userRegistrationDTO, mockResponse);
			// Then
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.CREATED), "Registration should return CREATED");
			assertNotNull(response.getBody());
			//Validate message
			UserAuthResponseDTO dto = response.getBody();
			assertEquals(Messages.USER_REGISTERED_SUCCESS, dto.getMessage().get("message"), "Wrong success message");
			//Validate access token
			assertEquals(fakeAccessToken, dto.getAccessToken(), "Access token not returned properly");
			//Validate refresh token in cookies
			String setCookieHeader = mockResponse.getHeader(HttpHeaders.SET_COOKIE);
			assertNotNull(setCookieHeader, "Refresh token cookie should be set");
			assertTrue(setCookieHeader.contains("refreshToken=" + fakeRefreshToken));
		}

		@Test
		@DisplayName("❌ Negative Case: Invalid email dns")
		void givenInvalidEmailDns_whenRegister_thenReturnEmailInvalidError() {
			// Given
			Mockito.when(dnsEmailLookup(anyString())).thenReturn(false);
			// When + Then
			UserRegistrationException ex = assertThrows(UserRegistrationException.class,
					() -> registerUser(userRegistrationDTO, mockResponse));
			assertEquals(Messages.EMAIL_INVALID_ERROR, ex.getErrors().get("email"));
		}

		@Test
		@DisplayName("❌ Negative Case: Already registered email")
		void givenExistingEmail_whenRegister_thenReturnEmailAlreadyExistError() {
			// Given
			Mockito.when(existsByEmail(anyString())).thenReturn(true);
			// When + Then
			UserRegistrationException ex = assertThrows(UserRegistrationException.class,
					() -> registerUser(userRegistrationDTO, mockResponse));
			// Validate error map
			assertNotNull(ex.getErrors(), "Errors map should not be null");
			assertEquals(Messages.EMAIL_ALREADY_EXISTS_ERROR, ex.getErrors().get("email"),
					"Wrong error message for already registered email.");
		}

		@Test
		@DisplayName("❌ Negative Case: Already existing username")
		void givenExistingUsername_whenRegister_thenReturnUsernameAlreadyExistError() {
			// Given
			Mockito.when(dnsEmailLookup(anyString())).thenReturn(true);
			Mockito.when(existsByUsername(anyString())).thenReturn(true);
			// When + Then
			UserRegistrationException ex = assertThrows(UserRegistrationException.class,
					() -> registerUser(userRegistrationDTO, mockResponse));
			// Validate error map
			assertNotNull(ex.getErrors(), "Errors map should not be null");
			assertEquals(Messages.USERNAME_ALREADY_EXISTS_ERROR,
					ex.getErrors().get("username"),
					"Wrong error message for already registered username.");
		}

		@Test
		@DisplayName("❌ Negative Case: Wrong confirm password")
		void givenWrongConfirmPassword_whenRegister_thenReturnInvalidConfirmPasswordError() {
			// Given
			Mockito.when(dnsEmailLookup(anyString())).thenReturn(true);
			userRegistrationDTO.setConfirmPassword("test");
			// When + Then
			UserRegistrationException ex = assertThrows(UserRegistrationException.class,
					() -> registerUser(userRegistrationDTO, mockResponse));
			// Validate error map
			assertNotNull(ex.getErrors(), "Errors map should not be null");
			assertEquals(Messages.INVALID_CONFIRM_PASSWORD_ERROR,
					ex.getErrors().get("password"),
					"Wrong error message for confirm password mismatch.");
		}

		@Test
		@DisplayName("❌ Negative Case: Invalid password encoder")
		void givenWrongPasswordEncoder_whenRegister_thenReturnServiceError() {
			// Given
			Mockito.when(dnsEmailLookup(anyString())).thenReturn(true);
			Mockito.when(passwordEncoder.encode(password)).thenReturn(password);
			// When + Then
			RuntimeException ex = assertThrows(RuntimeException.class,
					() -> registerUser(userRegistrationDTO, mockResponse));
			// Validate exception message
			assertEquals(Messages.SERVICE_ERROR, ex.getMessage(), "Wrong error message for invalid password encoder.");
		}

		@Test
		@DisplayName("❌ Negative Case: Multiple errors")
		void givenAllWrong_whenRegister_thenReturnMultipleErrors() {
			// Given
			Mockito.when(existsByEmail(anyString())).thenReturn(true);
			Mockito.when(existsByUsername(anyString())).thenReturn(true);
			userRegistrationDTO.setConfirmPassword("test");
			// When + Then
			UserRegistrationException ex = assertThrows(UserRegistrationException.class,
					() -> registerUser(userRegistrationDTO, mockResponse));
			// Validate error map
			Map<String, String> expectedErrors = Map.of(
					"email", Messages.EMAIL_ALREADY_EXISTS_ERROR,
					"username", Messages.USERNAME_ALREADY_EXISTS_ERROR,
					"password", Messages.INVALID_CONFIRM_PASSWORD_ERROR
			);
			assertNotNull(ex.getErrors(), "Errors map should not be null");
			assertEquals(expectedErrors, ex.getErrors(), "Wrong error messages for multiple validation failures.");
		}
	}

	@Nested
	@DisplayName("loginUser")
	class LoginUserTest {
		private UserLoginDTO userLoginDTO;
		private String password;

		private ResponseEntity<UserAuthResponseDTO> loginUser(UserLoginDTO userLoginDTO, HttpServletResponse response) {
			return authService.loginUser(userLoginDTO, response);
		}

		@BeforeEach
		void initEach() {
			String email = "test@intera.pl";
			password = "qWBRęLGć8MPł_test";
			userLoginDTO = new UserLoginDTO(email, password);
			mockResponse = new MockHttpServletResponse();
		}

		@Test
		@DisplayName("✅ Happy Case: User registered with valid credentials")
		void givenValidUserDTO_whenRegister_thenReturnUserRegisteredSuccess() {
			// Given
			User user = new User("TestUser", "test@intera.pl", password);
			Mockito.when(findByEmail(anyString())).thenReturn(Optional.of(user));
			Mockito.when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
			Mockito.when(refreshTokenService.createCookieWithRefreshToken(anyString(), any(User.class))).thenReturn(
					ResponseCookie.from("refreshToken", "dummyToken").httpOnly(true).build());
			Mockito.when(generateAccessToken(anyString())).thenReturn("dummyAccessToken");
			// When
			ResponseEntity<UserAuthResponseDTO> response = loginUser(userLoginDTO, mockResponse);
			// Then
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.OK), "Login failed");
			assertNotNull(response.getBody());
			assertEquals(Messages.LOGIN_SUCCESS, response.getBody().getMessage().get("message"), "Wrong success message");
			assertEquals("dummyAccessToken", response.getBody().getAccessToken(), "Access token mismatch");
		}

		@Test
		@DisplayName("❌ Negative Case: User not found")
		void givenNonExistingEmail_whenLogin_thenThrowUsernameNotFoundException() {
			// Given
			Mockito.when(findByEmail(userLoginDTO.getEmail())).thenReturn(Optional.empty());
			// When + Then
			UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
					() -> loginUser(userLoginDTO, mockResponse));
			assertEquals(Messages.INVALID_LOGIN_ERROR, ex.getMessage());
		}

		@Test
		@DisplayName("❌ Negative Case: Wrong password")
		void givenWrongPassword_whenLogin_thenThrowBadCredentialsException() {
			// Given
			User user = new User("TestUser", "test@intera.pl", "encodedPassword");
			Mockito.when(findByEmail(userLoginDTO.getEmail())).thenReturn(Optional.of(user));
			Mockito.when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
			// When + Then
			BadCredentialsException ex = assertThrows(BadCredentialsException.class,
					() -> loginUser(userLoginDTO, mockResponse));
			assertEquals(Messages.INVALID_LOGIN_ERROR, ex.getMessage());
		}
	}
}
