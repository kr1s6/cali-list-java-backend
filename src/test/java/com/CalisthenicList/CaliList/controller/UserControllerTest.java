package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.filter.UserValidationRateLimitingFilter;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.model.UserRegistrationDTO;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {

	private final String validUsername = "TestUser";
	private final String validEmail = "test@intera.pl";
	private final String validPassword = "qWBRę LGć8MPł test";
	private HttpHeaders headers;
	private UserRegistrationDTO userRegistrationDTO;
	private String postRegisterUrl;
	private String getEmailVerificationUrl;
	private String postLoginUrl;
	private String deleteUserByIdUrl;
	@Autowired
	private TestRestTemplate testRestTemplate;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@LocalServerPort
	private int port;
	@Autowired
	private UserValidationRateLimitingFilter filter;
	private int maxRequestsPerMinute;


	@BeforeEach
	void initAll() {
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		postRegisterUrl = "http://localhost:" + port + "/api/user/register";
		getEmailVerificationUrl = "http://localhost:" + port + "/api/user/email-verification/{token}";
		postLoginUrl = "http://localhost:" + port + "/api/user/login";
		deleteUserByIdUrl = "http://localhost:" + port + "/api/user/delete/{id}";
		userRegistrationDTO = new UserRegistrationDTO(validUsername, validEmail, validPassword, validPassword);
		filter = new UserValidationRateLimitingFilter();
		maxRequestsPerMinute = filter.MAX_HEAVY_REQUESTS_PER_MINUTE;
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

	//@Test
	//@DisplayName("❌ Negative Case: Valid email verification.")
	//void givenValidValues_whenSendingGetEmailVerification_thenReturnsCreatedSuccessfully() {
	//	// Given
	//	HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
	//	String userValidationUrl = "http://localhost:" + port + "/api/user/email-verification/invalidToken";
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
	//}

	@Test
	@DisplayName("✅ Happy Case: User registered with very long password")
	void givenVeryLongPassword_whenSendingPostRegister_thenReturnsCreatedSuccessfully() {
		// Given
		String veryLongPassword = "A1a".repeat(50); // 150 chars
		userRegistrationDTO.setPassword(veryLongPassword);
		userRegistrationDTO.setConfirmPassword(veryLongPassword);
		HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
		// When
		ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
		// Then
		assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.CREATED), "Should return Created.");
		assertTrue(userRepository.findByUsername(validUsername).isPresent(), "Warning! Username not found in DB");
		assertTrue(userRepository.findByEmail(validEmail).isPresent(), "Warning! Email not found in DB");
		User createdUser = userRepository.findByEmail(validEmail).get();
		assertTrue(passwordEncoder.matches(veryLongPassword, createdUser.getPassword()),
				"Warning! Password isn't properly encrypted");
	}

	@Test
	@DisplayName("❌ Negative Case: Block coming 'Post Register' requests after reaching requests limit.")
	void givenRequestsOverLimit_whenSendingPostRegister_thenReturnsTooManyRequestsError() {
		// Given
		HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
		// When
		for(int i = 0; i < maxRequestsPerMinute; i++) {
			ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
			assertFalse(response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS), "Shouldn't return Too many requests.");
		}
		ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
		// Then
		assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS), "Should return Too many requests.");
		String responseBody = response.getBody();
		assertEquals("Too many requests. Please try again later.", responseBody, "Wrong error message.");
	}

	@Test
	@DisplayName("❌ Negative Case: Block coming 'Get Email Verification' requests after reaching requests limit.")
	void givenRequestsOverLimit_whenSendingGetEmailVerification_thenReturnsTooManyRequestsError() {
		// Given
		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
		String userValidationUrl = "http://localhost:" + port + "/api/user/email-verification/invalidToken";
		// When
		for(int i = 0; i < maxRequestsPerMinute; i++) {
			ResponseEntity<String> response = testRestTemplate.exchange(userValidationUrl, HttpMethod.GET, requestEntity, String.class);
			assertFalse(response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS), "Shouldn't return Too many requests.");
		}
		ResponseEntity<String> response = testRestTemplate.exchange(userValidationUrl, HttpMethod.GET, requestEntity, String.class);
		// Then
		assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS), "Should return Too many requests.");
		String responseBody = response.getBody();
		assertEquals("Too many requests. Please try again later.", responseBody, "Wrong error message.");
	}


	//@Nested
	//@DisplayName("User registration tests")
	//class UserControllerTestRegister {
	//
	//	@Test
	//	@DisplayName("❌ Negative Case: You can't send more than 5 DELETE requests during 1 min.")
	//	void givenFiveDeleteRequestsDuringOneMin_WhenSendingSixthRequest_ThenRateLimitingFilterRejectsRequest() {
	//		// Given
	//		String deleteUserUrl = "http://localhost:" + port + "/api/user/delete/" + UUID.randomUUID();
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
	//				"Warning! Too many requests passed for user during 1 minute. Code: " + response.getStatusCode());
	//	}
	//
	//
	//
	//	@Test
	//	@DisplayName("❌ Negative Case: User can't send more than 5 LOGIN requests during 1 min.")
	//	void givenFiveLoginRequestsDuringOneMin_WhenSendingSixthRequest_ThenRateLimitingFilterRejectsRequest() {
	//		// Given
	//		String postLoginUrl = "http://localhost:" + port + "/api/user/login";
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
	//				"Warning! Too many requests passed for user during 1 minute. Code: " + response.getStatusCode());
	//	}

}
