package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.enums.Roles;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.model.UserLoginDTO;
import com.CalisthenicList.CaliList.model.UserRegistrationDTO;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import com.CalisthenicList.CaliList.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.post;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WebMvcTest(UserController.class)
@ActiveProfiles("test")
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

	@Autowired
	private MockMvc mockMvc;
	@MockBean
	private UserService userService;

	@BeforeAll
	static void init() {
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
	}

		@Test
		void shouldReturn200WhenUserRegistered() throws Exception {
			when(userService.register("Ala")).thenReturn(new User("Ala"));

			mockMvc.perform(post("/api/user/register")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"username\":\"Ala\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.name").value("Ala"));
		}
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

		@Test
		@DisplayName("❌ Negative Case: You can't send more than 5 DELETE requests during 1 min.")
		void givenFiveDeleteRequestsDuringOneMin_WhenSendingSixthRequest_ThenRateLimitingFilterRejectsRequest() {
			// Given
			String deleteUserUrl = "http://localhost:" + port + "/api/user/delete/" + UUID.randomUUID();
			headers.set("X-Forwarded-For", "10.0.0.5");
			HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
			ResponseEntity<String> response;
			for(int i = 0; i < MAX_HEAVY_REQUESTS_PER_MINUTE; i++) {
				response = testRestTemplate.exchange(deleteUserUrl, HttpMethod.DELETE, requestEntity, String.class);
				assertTrue(response.getStatusCode().is4xxClientError());
			}
			// When
			response = testRestTemplate.exchange(deleteUserUrl, HttpMethod.DELETE, requestEntity, String.class);
			// Then
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS),
					"Warning! Too many requests passed for user during 1 minute. Code: " + response.getStatusCode());
		}

		@Test
		@DisplayName("❌ Negative Case: User can't send more than 5 EMAIL_VERIFICATION requests during 1 min.")
		void givenFiveEmailVerificationRequestsDuringOneMin_WhenSendingSixthRequest_ThenRateLimitingFilterRejectsRequest() {
			// Given
			String userValidationUrl = "http://localhost:" + port + "/api/user/email-verification/invalidToken";
			headers.set("X-Forwarded-For", "10.0.0.6");
			HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
			ResponseEntity<String> response;
			for(int i = 0; i < MAX_HEAVY_REQUESTS_PER_MINUTE; i++) {
				response = testRestTemplate.exchange(userValidationUrl, HttpMethod.GET, requestEntity, String.class);
				assertTrue(response.getStatusCode().is4xxClientError());
			}
			// When
			response = testRestTemplate.exchange(userValidationUrl, HttpMethod.GET, requestEntity, String.class);
			// Then
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS),
					"Warning! Too many requests passed for user during 1 minute. Code: " + response.getStatusCode());
		}

		@Test
		@DisplayName("❌ Negative Case: User can't send more than 5 LOGIN requests during 1 min.")
		void givenFiveLoginRequestsDuringOneMin_WhenSendingSixthRequest_ThenRateLimitingFilterRejectsRequest() {
			// Given
			String postLoginUrl = "http://localhost:" + port + "/api/user/login";
			headers.set("X-Forwarded-For", "10.0.0.7");
			UserLoginDTO userLoginDTO = new UserLoginDTO();
			HttpEntity<UserLoginDTO> invalidLoginRequest = new HttpEntity<>(userLoginDTO, headers);
			ResponseEntity<String> response;
			for(int i = 0; i < MAX_HEAVY_REQUESTS_PER_MINUTE; i++) {
				response = testRestTemplate.postForEntity(postLoginUrl, invalidLoginRequest, String.class);
				assertTrue(response.getStatusCode().is4xxClientError());
			}
			// When
			response = testRestTemplate.postForEntity(postLoginUrl, invalidLoginRequest, String.class);
			// Then
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS),
					"Warning! Too many requests passed for user during 1 minute. Code: " + response.getStatusCode());
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
				userRegistrationDTO.setConfirmPassword(veryLongPassword);
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


		}

	}
}