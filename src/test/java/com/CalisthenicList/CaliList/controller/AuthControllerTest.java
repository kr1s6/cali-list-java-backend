package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.constants.UserConstants;
import com.CalisthenicList.CaliList.filter.UserValidationRateLimitingFilter;
import com.CalisthenicList.CaliList.model.UserRegistrationDTO;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerTest {

	@Autowired
	private TestRestTemplate testRestTemplate;
	@LocalServerPort
	private int port;
	private HttpHeaders headers;
	private UserRegistrationDTO userRegistrationDTO;
	private String postRegisterUrl;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private UserValidationRateLimitingFilter filter;
	private int maxRequestsPerMinute;


	@BeforeEach
	void initEach() {
		filter = new UserValidationRateLimitingFilter();
		postRegisterUrl = "http://localhost:" + port + "/register";
		maxRequestsPerMinute = filter.MAX_HEAVY_REQUESTS_PER_MINUTE;
		//-----Headers-----
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String csrfUrl = "http://localhost:" + port + "/csrf";
		ResponseEntity<Map> response = testRestTemplate.exchange(csrfUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
		String token = response.getBody().get("token").toString();
		String headerName = response.getBody().get("headerName").toString();
		headers.set(headerName, token);
		List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
		if(cookies != null && !cookies.isEmpty()) {
			String jsession = cookies.get(0).split(";", 2)[0];
			headers.set(HttpHeaders.COOKIE, jsession);
		}
		//-----Headers-----
	}

	@Nested
	@DisplayName("/register")
	class Register {
		private final String validUsername = "TestUser";
		private final String validEmail = "test@intera.pl";

		@BeforeEach
		void initEach() {
			String validPassword = "qWBRę LGć8MPł test";
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
		@DisplayName("✅ Happy Case: User created with valid credentials")
		void givenValidValues_whenSendingPostRegister_thenReturnsCreatedSuccessfully() {
			// Given
			HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
			// When
			ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
			// Then
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.CREATED), "Should return Created.");
			assertTrue(userRepository.findByUsername(validUsername).isPresent(), "Warning! Username not found in DB");
			assertTrue(userRepository.findByEmail(validEmail).isPresent(), "Warning! Email not found in DB");
		}

		@Test
		@DisplayName("✅ Happy Case: Max length password")
		void givenMaxLengthPassword_whenSendingPostRegister_thenReturnsCreatedSuccessfully() {
			// Given
			String veryLongPassword = "a".repeat(UserConstants.PASSWORD_MAX_LENGTH);
			userRegistrationDTO.setPassword(veryLongPassword);
			userRegistrationDTO.setConfirmPassword(veryLongPassword);
			HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
			// When
			ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
			// Then
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.CREATED), "Should return Created.");
			assertTrue(userRepository.findByUsername(validUsername).isPresent(), "Warning! Username not found in DB");
			assertTrue(userRepository.findByEmail(validEmail).isPresent(), "Warning! Email not found in DB");
		}

		@Test
		@DisplayName("❌ Negative Case: Too long password")
		void givenTooLongPassword_whenSendingPostRegister_thenReturnsBadRequestError() {
			// Given
			String tooLongPassword = "a".repeat(UserConstants.PASSWORD_MAX_LENGTH + 1);
			userRegistrationDTO.setPassword(tooLongPassword);
			userRegistrationDTO.setConfirmPassword(tooLongPassword);
			HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
			// When
			ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
			// Then
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST), "Should return Bad Request.");
			assertFalse(userRepository.findByUsername(validUsername).isPresent(), "Warning! Username not found in DB");
			assertFalse(userRepository.findByEmail(validEmail).isPresent(), "Warning! Email not found in DB");
		}

		//@Test
		//@DisplayName("❌ Negative Case: Block coming 'Post Register' requests after reaching requests limit.")
		//void givenRequestsOverLimit_whenSendingPostRegister_thenReturnsTooManyRequestsError() throws InterruptedException {
		//	// Given
		//	HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
		//	// When
		//	for(int i = 0; i < maxRequestsPerMinute; i++) {
		//		ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
		//		assertFalse(response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS), "Shouldn't return Too many requests.");
		//	}
		//	ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
		//	// Then
		//	assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS), "Should return Too many requests.");
		//	String responseBody = response.getBody();
		//	assertEquals("Too many requests. Please try again later.", responseBody, "Wrong error message.");
		//	Thread.sleep(filter.REFILL_PERIOD);
		//}
	}

	@Nested
	@DisplayName("/email-verification/{token}")
	class EmailVerification {

		//@Test
		//@DisplayName("❌ Negative Case: Invalid email verification.")
		//void givenInvalidToken_whenSendingGetEmailVerification_thenReturnsBadRequestError() {
		//	// Given
		//	HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
		//	String getEmailVerificationUrl = "http://localhost:" + port + "/email-verification/invalidToken";
		//	// When
		//	ResponseEntity<String> response = testRestTemplate.exchange(getEmailVerificationUrl, HttpMethod.GET, requestEntity, String.class);
		//	// Then
		//	assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST), "Should return Bad Request.");
		//	String responseBody = response.getBody();
		//	assertEquals(Messages.TOKEN_INVALID, responseBody, "Wrong error message.");
		//}

		//@Test
		//@DisplayName("❌ Negative Case: Block coming 'Get Email Verification' requests after reaching requests limit.")
		//void givenRequestsOverLimit_whenSendingGetEmailVerification_thenReturnsTooManyRequestsError() throws InterruptedException {
		//	// Given
		//	HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
		//	String userValidationUrl = "http://localhost:" + port + "/email-verification/invalidToken";
		//	// When
		//	for(int i = 0; i < maxRequestsPerMinute; i++) {
		//		ResponseEntity<String> response = testRestTemplate.exchange(userValidationUrl, HttpMethod.GET, requestEntity, String.class);
		//		assertFalse(response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS), "Shouldn't return Too many requests.");
		//	}
		//	ResponseEntity<String> response = testRestTemplate.exchange(userValidationUrl, HttpMethod.GET, requestEntity, String.class);
		//	// Then
		//	assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS), "Should return Too many requests.");
		//	String responseBody = response.getBody();
		//	assertEquals("Too many requests. Please try again later.", responseBody, "Wrong error message.");
		//	Thread.sleep(filter.REFILL_PERIOD);
		//}
	}

	@Nested
	@DisplayName("/login")
	class Login {

		private String postLoginUrl;

		@BeforeEach
		void initEach() {
			postLoginUrl = "http://localhost:" + port + "/login";

		}

		//	@Test
		//	@DisplayName("❌ Negative Case: User can't send more than 5 LOGIN requests during 1 min.")
		//	void givenFiveLoginRequestsDuringOneMin_WhenSendingSixthRequest_ThenRateLimitingFilterRejectsRequest() {
		//		// Given
		//		String postLoginUrl = "http://localhost:" + port + "/login";
		//		headers.set("X-Forwarded-For", "10.0.0.7");
		//		UserLoginDTO userLoginDTO = new UserLoginDTO();
		//		HttpEntity<UserLoginDTO> invalidLoginRequest = new HttpEntity<>(userLoginDTO, headers);
		//		ResponseEntity<String> response;
		//		for(int i = 0; i < MAX_HEAVY_REQUESTS_PER_MINUTE; i++) {
		//			response = testRestTemplate.postForEntity(postLoginUrl, invalidLoginRequest, String.class);
		//			assertTrue(response.getStatusCode().is4xxClientError());
		//		}
		//		// When
		//		response = testRestTemplate.postForEntity(postLoginUrl, invalidLoginRequest, String.class);
		//		// Then
		//		assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS),
		//				"Warning! Too many requests passed for the user during 1 minute. Code: " + response.getStatusCode());
		//	}

	}

	@Nested
	@DisplayName("/delete/{id}")
	class Delete {
		private String deleteUserByIdUrl;

		@BeforeEach
		void initEach() {
			deleteUserByIdUrl = "http://localhost:" + port + "/delete/{id}";
		}
		//	@Test
		//	@DisplayName("❌ Negative Case: You can't send more than 5 DELETE requests during 1 min.")
		//	void givenFiveDeleteRequestsDuringOneMin_WhenSendingSixthRequest_ThenRateLimitingFilterRejectsRequest() {
		//		// Given
		//		String deleteUserUrl = "http://localhost:" + port + "/delete/" + UUID.randomUUID();
		//		headers.set("X-Forwarded-For", "10.0.0.5");
		//		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
		//		ResponseEntity<String> response;
		//		for(int i = 0; i < MAX_HEAVY_REQUESTS_PER_MINUTE; i++) {
		//			response = testRestTemplate.exchange(deleteUserUrl, HttpMethod.DELETE, requestEntity, String.class);
		//			assertTrue(response.getStatusCode().is4xxClientError());
		//		}
		//		// When
		//		response = testRestTemplate.exchange(deleteUserUrl, HttpMethod.DELETE, requestEntity, String.class);
		//		// Then
		//		assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS),
		//				"Warning! Too many requests passed for the user during 1 minute. Code: " + response.getStatusCode());
		//	}
	}
}
