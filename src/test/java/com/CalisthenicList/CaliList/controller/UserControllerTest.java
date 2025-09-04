package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.enums.Roles;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.model.UserRegistrationDTO;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import com.CalisthenicList.CaliList.service.UserControllerService;
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
	private UserRegistrationDTO userRegistrationDTO;

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
		private final String validPassword = "qWBRę LGć8MPł test";
		private String postRegisterUrl;

		@BeforeEach
		void initAll() {
			//        Given
			postRegisterUrl = "http://localhost:" + port + "/api/user/register";
			userRegistrationDTO = new UserRegistrationDTO(validUsername, validEmail, validPassword, validPassword);
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
			HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
			//        When
			ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
			//        Then
			assertTrue(response.getStatusCode().is2xxSuccessful(), "Warning! Registration failed. Code: " + response.getStatusCode());
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
				userRegistrationDTO.setEmail(invalidEmail);
				HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
				// When
				ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
				// Then
				assertTrue(response.getStatusCode().is4xxClientError(), "Warning! Registration passed with invalid email.");
				assertNotNull(response.getBody());
				assertTrue(response.getBody().contains(Messages.EMAIL_INVALID_ERROR), "Warning! Wrong error message");
				assertFalse(userRepository.findByEmail(invalidEmail).isPresent(), "Warning! Email should not exist in DB");
			}

			@DisplayName("❌ Negative Case: Invalid email domain")
			@ParameterizedTest(name = "Invalid email domain case: \"{0}\"")
			@ValueSource(strings = {"invalid@34534sdfgsdfs.com", "invalid@gmial.com"})
			void givenInvalidEmailDomain_WhenSendingPostRequest_ThenUserIsNotCreated(String invalidEmail) {
				// Given
				userRegistrationDTO.setEmail(invalidEmail);
				HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
				// When
				ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
				// Then
				assertTrue(response.getStatusCode().is4xxClientError(), "Warning! Registration passed with invalid email.");
				assertNotNull(response.getBody());
				assertTrue(response.getBody().contains(Messages.EMAIL_INVALID_ERROR), "Warning! Wrong error message");
				assertFalse(userRepository.findByEmail(invalidEmail).isPresent(), "Warning! User should not exist in DB");
			}

			@Test
			@DisplayName("❌ Negative Case: Already registered email")
			void givenExistingEmail_WhenSendingPostRequest_ThenUserIsNotCreated() {
				// Given
				String notRepeatableUsername = "DifferentUsername";
				UserRegistrationDTO userDTO = new UserRegistrationDTO(notRepeatableUsername, validEmail, validPassword, validPassword);
				User testUser = UserControllerService.toUser(userDTO);
				userRepository.save(testUser);
				HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
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
				userRegistrationDTO.setUsername(longUsername);
				HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
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
				userRegistrationDTO.setUsername(invalidUsername);
				HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
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
				String notRepeatableEmail = "different@user.com";
				UserRegistrationDTO validUserDto = new UserRegistrationDTO(validUsername, notRepeatableEmail, validPassword, validPassword);
				User user = UserControllerService.toUser(validUserDto);
				userRepository.save(user);
				HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
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

			@Test
			@DisplayName("✅ Happy Case: User registered with long password")
			void givenLongPassword_WhenSendingPostRequest_ThenUserIsCreated() {
				// Given
				String veryLongPassword = "A1a".repeat(50); // 150 chars
				userRegistrationDTO.setPassword(veryLongPassword);
				userRegistrationDTO.setRepeatedPassword(veryLongPassword);
				HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
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
			
			@DisplayName("❌ Negative Case: Blank password")
			@ParameterizedTest(name = "Invalid password case: \"{0}\"")
			@NullAndEmptySource
			@ValueSource(strings = {"        "})
			void givenBlankPassword_WhenSendingPostRequest_ThenUserIsNotCreated(String invalidPassword) {
				//Given
				userRegistrationDTO.setPassword(invalidPassword);
				userRegistrationDTO.setRepeatedPassword(invalidPassword);
				HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
				//When
				ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
				//Then
				assertTrue(response.getStatusCode().is4xxClientError(),
						"Warning! Registration passed with invalid password. Code: " + response.getStatusCode());
				assertNotNull(response.getBody());
				assertTrue(response.getBody().contains(Messages.PASSWORD_NOT_BLANK_ERROR), "Warning! Wrong error message");
				assertFalse(userRepository.findByEmail(userRegistrationDTO.getEmail()).isPresent(), "Warning! Email found in DB");
			}

			@Test
			@DisplayName("❌ Negative Case: Too short password")
			void givenTooShortPassword_WhenSendingPostRequest_ThenUserIsNotCreated() {
				// Given
				userRegistrationDTO.setPassword("Invalid");
				userRegistrationDTO.setRepeatedPassword("Invalid");
				HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
				// When
				ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
				// Then
				assertTrue(response.getStatusCode().is4xxClientError(),
						"Warning! Registration passed with too short password. Code: " + response.getStatusCode());
				assertFalse(userRepository.findByEmail(userRegistrationDTO.getEmail()).isPresent(), "Warning! Email found in DB");
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

		//		@BeforeEach
		//		void initAll() {
		//			deleteUserUrl = "http://localhost:" + port + "/api/user/delete/";
		//			user = new User();
		//		}
		//		TODO DO zrobienia testy
		//		testRestTemplate.delete(deleteUserUrl + createdUser.getId());
	}
}