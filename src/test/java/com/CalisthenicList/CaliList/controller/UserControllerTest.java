package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.enums.Roles;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//INFO SpringBootTest is heavy weight and should be used only for integration tests when whole Spring context is needed
class UserControllerTest {
	private static HttpHeaders headers;
	@LocalServerPort
	private int port;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private TestRestTemplate testRestTemplate;
	@Autowired
	private PasswordEncoder passwordEncoder;
	private User user;

	@BeforeAll
	static void init() {
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
	}

	@Nested
	@DisplayName("User registration tests")
	class UserControllerTestRegister {
		private final String validUsername = "TestUser";
		private final String validEmail = "test@intera.pl";
		private final String validPassword = "qWBRę LGć8MPł  pass";
		private String postRegisterUrl;

		@BeforeEach
		void initAll() {
			postRegisterUrl = "http://localhost:" + port + "/api/user/register";
			user = new User();
		}

		@AfterEach
		void cleanUp() {
			if(userRepository.findByEmail(validEmail).isPresent()) {
				userRepository.delete(userRepository.findByEmail(validEmail).get());
			}
			if(userRepository.findByUsername(validUsername).isPresent()) {
				userRepository.delete(userRepository.findByUsername(validUsername).get());
			}
		}

		@Test
		@DisplayName("✅ Happy Case: User registered with valid credentials")
		void givenValidValues_WhenSendingPostRequest_ThenSuccessfullyCreatedUserInDB() {
//        Given
			user.setUsername(validUsername);
			user.setEmail(validEmail);
			user.setPassword(validPassword);
			HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
//        When
			ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
//        Then
			assertTrue(response.getStatusCode().is2xxSuccessful(),
					"Warning! Registration failed. Code: " + response.getStatusCode());
			assertTrue(userRepository.findByUsername(validUsername).isPresent(), "Warning! Username not found in DB");
			assertTrue(userRepository.findByEmail(validEmail).isPresent(), "Warning! Email not found in DB");
			User createdUser = userRepository.findByEmail(validEmail).get();
			assertNotNull(createdUser.getCreatedDate(), "Warning! Created Date is null");
			assertNotNull(createdUser.getUpdatedDate(), "Warning! Updated Date is null");
			assertEquals(Roles.ROLE_USER, createdUser.getRole(), "Warning! User has wrong role.");
			assertTrue(passwordEncoder.matches(validPassword, createdUser.getPassword()),
					"Warning! Password isn't properly encrypted");
		}

		@Nested
		@DisplayName("Email tests")
		class EmailTests {
			@DisplayName("❌ Negative Case: Invalid email")
			@ParameterizedTest(name = "Invalid email case: \"{0}\"")
			@NullAndEmptySource
			@ValueSource(strings = {"        ", "invalid-email-format"})
			void givenInvalidEmail_WhenSendingPostRequest_ThenUserIsNotCreated(String invalidEmail) {
				// Given
				user.setUsername(validUsername);
				user.setEmail(invalidEmail);
				user.setPassword(validPassword);
				HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
				// When
				ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
				// Then
				assertTrue(response.getStatusCode().is4xxClientError(),
						"Warning! Registration passed with invalid email. Code: " + response.getStatusCode());
				assertNotNull(response.getBody());
				assertTrue(response.getBody().contains(Messages.EMAIL_INVALID_ERROR), "Warning! Wrong error message");
				assertFalse(userRepository.findByEmail(invalidEmail).isPresent(), "Warning! Email should not exist in DB");
			}

			@DisplayName("❌ Negative Case: Invalid email domain")
			@ParameterizedTest(name = "Invalid email domain case: \"{0}\"")
			@ValueSource(strings = {"invalid@34534sdfgsdfs.com", "invalid@gmial.com"})
			void givenInvalidEmailDomain_WhenSendingPostRequest_ThenUserIsNotCreated(String invalidEmail) {
				// Given
				user.setUsername(validUsername);
				user.setEmail(invalidEmail); // no @
				user.setPassword(validPassword);
				HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
				// When
				ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
				// Then
				assertTrue(response.getStatusCode().is4xxClientError(),
						"Warning! Registration passed with invalid email. Code: " + response.getStatusCode());
				assertNotNull(response.getBody());
				assertTrue(response.getBody().contains(Messages.EMAIL_INVALID_ERROR), "Warning! Wrong error message");
				assertFalse(userRepository.findByEmail(invalidEmail).isPresent(), "Warning! User should not exist in DB");
			}

			@Test
			@DisplayName("❌ Negative Case: Already registered email")
			void givenExistingEmail_WhenSendingPostRequest_ThenUserIsNotCreated() {
				// Given
				User validUser = new User();
				validUser.setUsername("DifferentUsername");
				validUser.setEmail(validEmail);
				validUser.setPassword(validPassword);
				userRepository.save(validUser);
				user.setUsername(validUsername);
				user.setEmail(validEmail);
				user.setPassword(validPassword);
				HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
				// When
				ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
				// Then
				assertTrue(response.getStatusCode().is4xxClientError(),
						"Warning! Registration passed with duplicate email. Code: " + response.getStatusCode());
				assertNotNull(response.getBody());
				assertTrue(response.getBody().contains(Messages.EMAIL_ALREADY_EXISTS_ERROR), "Warning! Wrong error message");
				assertFalse(userRepository.findByUsername(validUsername).isPresent(), "Warning! User should not exist in DB");
			}
		}

		@Nested
		@DisplayName("Username tests")
		class UsernameTests {
			@Test
			@DisplayName("❌ Negative Case: Too long username")
			void givenTooLongUsername_WhenSendingPostRequest_ThenUserIsNotCreated() {
				// Given
				String longUsername = "A".repeat(21); // 21 chars
				user.setUsername(longUsername);
				user.setEmail(validEmail);
				user.setPassword(validPassword);
				HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
				// When
				ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
				// Then
				assertTrue(response.getStatusCode().is4xxClientError(),
						"Warning! Registration passed with too long username. Code: " + response.getStatusCode());
				assertFalse(userRepository.findByUsername(longUsername).isPresent(), "Warning! User should not exist in DB");
				assertNotNull(response.getBody());
				assertTrue(response.getBody().contains(Messages.USERNAME_LENGTH_ERROR), "Warning! Wrong error message");
			}

			@DisplayName("❌ Negative Case: Blank username")
			@ParameterizedTest(name = "Invalid username case: \"{0}\"")
			@NullAndEmptySource
			@ValueSource(strings = {"        "})
			void givenBlankUsername_WhenSendingPostRequest_ThenUserIsNotCreated(String invalidUsername) {
				// Given
				user.setUsername(invalidUsername);
				user.setEmail(validEmail);
				user.setPassword(validPassword);
				HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
				// When
				ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
				// Then
				assertTrue(response.getStatusCode().is4xxClientError(),
						"Warning! Registration passed with invalid username. Code: " + response.getStatusCode());
				assertFalse(userRepository.findByUsername(invalidUsername).isPresent(), "Warning! User should not exist in DB");
				assertNotNull(response.getBody());
				assertTrue(response.getBody().contains(Messages.USERNAME_NOT_BLANK_ERROR), "Warning! Wrong error message");
			}

			@Test
			@DisplayName("❌ Negative Case: Already existing username")
			void givenExistingUsername_WhenSendingPostRequest_ThenUserIsNotCreated() {
				// Given
				User validUser = new User();
				validUser.setUsername(validUsername);
				validUser.setEmail("different@user.com");
				validUser.setPassword(validPassword);
				userRepository.save(validUser);
				user.setUsername(validUsername);
				user.setEmail(validEmail);
				user.setPassword(validPassword);
				HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
				// When
				ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
				// Then
				assertTrue(response.getStatusCode().is4xxClientError(),
						"Warning! Registration passed with duplicate username. Code: " + response.getStatusCode());
				assertNotNull(response.getBody());
				assertTrue(response.getBody().contains(Messages.USERNAME_ALREADY_EXISTS_ERROR), "Warning! Wrong error message");
				assertFalse(userRepository.findByEmail(validEmail).isPresent(), "Warning! User should not exist in DB");
			}
		}

		@Nested
		@DisplayName("Password tests")
		class PasswordTests {
			@DisplayName("❌ Negative Case: Blank password")
			@ParameterizedTest(name = "Invalid password case: \"{0}\"")
			@NullAndEmptySource
			@ValueSource(strings = {"        "})
			void givenBlankPassword_WhenSendingPostRequest_ThenUserIsNotCreated(String invalidPassword) {
				//Given
				user.setUsername(validUsername);
				user.setEmail(validEmail);
				user.setPassword(invalidPassword);
				HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
				//When
				ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
				//Then
				assertTrue(response.getStatusCode().is4xxClientError(),
						"Warning! Registration passed with invalid password. Code: " + response.getStatusCode());
				assertNotNull(response.getBody());
				assertTrue(response.getBody().contains(Messages.PASSWORD_NOT_BLANK_ERROR), "Warning! Wrong error message");
				assertFalse(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email found in DB");
			}

			@Test
			@DisplayName("✅ Happy Case: User registered with long password")
			void givenLongPassword_WhenSendingPostRequest_ThenUserIsCreated() {
				// Given
				String veryLongPassword = "A1a".repeat(50); // 150 chars
				user.setUsername(validUsername);
				user.setEmail(validEmail);
				user.setPassword(veryLongPassword);
				HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
				// When
				ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
				// Then
				assertTrue(response.getStatusCode().is2xxSuccessful(),
						"Warning! Registration failed. Code: " + response.getStatusCode());
				assertTrue(userRepository.findByEmail(validEmail).isPresent(), "Warning! Email not found in DB");
				User createdUser = userRepository.findByEmail(validEmail).get();
				assertTrue(passwordEncoder.matches(veryLongPassword, createdUser.getPassword()),
						"Warning! Password isn't properly encrypted");
			}

			@Test
			@DisplayName("❌ Negative Case: Too short password")
			void givenTooShortPassword_WhenSendingPostRequest_ThenUserIsNotCreated() {
				// Given
				user.setUsername(validUsername);
				user.setEmail(validEmail);
				user.setPassword("Invalid");
				HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
				// When
				ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
				// Then
				assertTrue(response.getStatusCode().is4xxClientError(),
						"Warning! Registration passed with too short password. Code: " + response.getStatusCode());
				assertFalse(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email found in DB");
				assertNotNull(response.getBody());
				assertTrue(response.getBody().contains(Messages.PASSWORD_LENGTH_ERROR), "Warning! Wrong error message");
			}
		}
	}

	@Nested
	@DisplayName("User login tests")
	class UserControllerTestLogin {
	}

	@Nested
	@DisplayName("Delete user tests")
	class UserControllerTestDeleteUser {
		private String deleteUserUrl;

		@BeforeEach
		void initAll() {
			deleteUserUrl = "http://localhost:" + port + "/api/user/delete/";
			user = new User();
		}
//		TODO DO zrobienia testy
//		testRestTemplate.delete(deleteUserUrl + createdUser.getId());
	}
}