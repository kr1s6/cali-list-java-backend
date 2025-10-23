package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.constants.UserConstants;
import com.CalisthenicList.CaliList.enums.Roles;
import com.CalisthenicList.CaliList.filter.UserValidationRateLimitingFilter;
import com.CalisthenicList.CaliList.model.RefreshToken;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.model.UserLoginDTO;
import com.CalisthenicList.CaliList.model.UserRegistrationDTO;
import com.CalisthenicList.CaliList.repositories.RefreshTokenRepository;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerTest {

	@LocalServerPort
	private int port;
	private HttpHeaders headers;
	private UserRegistrationDTO userRegistrationDTO;
	private UserLoginDTO userLoginDTO;
	private String postRegisterUrl;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private RefreshTokenRepository refreshTokenRepository;
	@Autowired
	private UserValidationRateLimitingFilter filter;
	private int maxRequestsPerMinute;

	private Optional<User> findByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	private Optional<User> findByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	private Optional<RefreshToken> findByUserEmail(String email) {
		return refreshTokenRepository.findByUserEmail(email);
	}

	@BeforeEach
	void initEach() {
		filter = new UserValidationRateLimitingFilter();
		postRegisterUrl = "http://localhost:" + port + AuthController.registerUrl;
		maxRequestsPerMinute = filter.MAX_HEAVY_REQUESTS_PER_MINUTE;
		//-----Headers-----
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
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
			if(findByUserEmail(validEmail).isPresent()) {
				refreshTokenRepository.delete(findByUserEmail(validEmail).get());
			}
			if(findByEmail(validEmail).isPresent()) {
				userRepository.delete(findByEmail(validEmail).get());
			}
			if(findByUsername(validUsername).isPresent()) {
				userRepository.delete(findByUsername(validUsername).get());
			}
		}

		@Test
		@DisplayName("✅ Happy Case: User created with valid credentials")
		void givenValidValues_whenSendingPostRegister_thenReturnsCreatedSuccessfully() {
			RestAssured.given()
					.body(userRegistrationDTO)
					.auth().oauth2(validUsername)
					.headers(headers)
					.log().all()
					.when()
					.post(postRegisterUrl)
					.then()
					.log().all()
					.statusCode(HttpStatus.CREATED.value())
					.body("accessToken", Matchers.notNullValue())
					.body("success", Matchers.equalTo(true))
					.body("message", Matchers.equalTo(Messages.USER_REGISTERED_SUCCESS))
					.body("data.id", Matchers.notNullValue())
					.body("data.role", Matchers.equalTo(Roles.ROLE_USER.name()))
					.body("data.username", Matchers.equalTo(validUsername))
					.body("data.email", Matchers.equalTo(validEmail))
					.body("data.emailVerified", Matchers.equalTo(false));
			assertTrue(findByUsername(validUsername).isPresent(), "Warning! Username not found in DB");
			assertTrue(findByEmail(validEmail).isPresent(), "Warning! Email not found in DB");
		}

		@Test
		@DisplayName("✅ Happy Case: Max length password")
		void givenMaxLengthPassword_whenSendingPostRegister_thenReturnsCreatedSuccessfully() {
			String veryLongPassword = "a".repeat(UserConstants.PASSWORD_MAX_LENGTH);
			userRegistrationDTO.setPassword(veryLongPassword);
			userRegistrationDTO.setConfirmPassword(veryLongPassword);
			RestAssured.given()
					.body(userRegistrationDTO)
					.headers(headers)
					.log().all()
					.when()
					.post(postRegisterUrl)
					.then()
					.log().all()
					.statusCode(HttpStatus.CREATED.value())
					.body("success", Matchers.equalTo(true))
					.body("message", Matchers.equalTo(Messages.USER_REGISTERED_SUCCESS));
			assertTrue(findByEmail(validEmail).isPresent(), "Warning! Email not found in DB");
		}

		@Test
		@DisplayName("❌ Negative Case: Too long password")
		void givenTooLongPassword_whenSendingPostRegister_thenReturnsBadRequestError() {
			String tooLongPassword = "a".repeat(UserConstants.PASSWORD_MAX_LENGTH + 1);
			userRegistrationDTO.setPassword(tooLongPassword);
			userRegistrationDTO.setConfirmPassword(tooLongPassword);
			RestAssured.given()
					.body(userRegistrationDTO)
					.headers(headers)
					.log().all()
					.when()
					.post(postRegisterUrl)
					.then()
					.log().all()
					.statusCode(HttpStatus.BAD_REQUEST.value())
					.body("success", Matchers.equalTo(false))
					.body("accessToken", Matchers.notNullValue())
					.body("message", Matchers.equalTo("Validation failed."));
			assertFalse(findByUsername(validUsername).isPresent(), "Warning! Username not found in DB");
			assertFalse(findByEmail(validEmail).isPresent(), "Warning! Email not found in DB");
		}
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
		private final String validEmail = "test@intera.pl";
		private final String validPassword = "qWBRę LGć8MPł test";
		private String postLoginUrl;

		@BeforeEach
		void initEach() {
			postLoginUrl = "http://localhost:" + port + "/login";
			headers.set("X-Forwarded-For", "10.0.0.7");
			userLoginDTO = new UserLoginDTO(validEmail, validPassword);
		}

		@Test
		@DisplayName("❌ Negative Case: Block coming 'Post Register' requests after reaching requests limit.")
		void givenRequestsOverLimit_whenSendingPostRegister_thenReturnsTooManyRequestsError() throws InterruptedException {
			for(int i = 0; i < maxRequestsPerMinute; i++) {
				int statusCode = RestAssured.given()
						.body(userLoginDTO).headers(headers)
						.when().post(postLoginUrl)
						.then().extract().statusCode();
				assertNotEquals(HttpStatus.TOO_MANY_REQUESTS.value(), statusCode, "Shouldn't return Too many requests.");
			}
			RestAssured.given()
					.body(userLoginDTO).headers(headers)
					.when().post(postLoginUrl)
					.then()
					.log().all()
					.statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
					.body("body", Matchers.equalTo("Too many requests. Please try again later."));
			Thread.sleep(filter.REFILL_PERIOD);
		}
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
