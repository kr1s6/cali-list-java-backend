package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.model.RefreshToken;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.model.UserDeleteByIdDTO;
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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerTest {
	@LocalServerPort
	private int port;
	private HttpHeaders headers;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final String initEmail = "Inittest@interia.pl";
	private final String initPassword = "Init password test";
	private UUID userId;

	@Autowired
	public UserControllerTest(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
	}

	private Optional<User> findUserByEmail() {
		return userRepository.findByEmail(initEmail);
	}

	private Optional<RefreshToken> findRefTokenByEmail() {
		return refreshTokenRepository.findByUserEmail(initEmail);
	}

	@BeforeEach
	void initBeforeEach() {
		//-----Headers-----
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Forwarded-For", "10.0.0.7");
		//-----Headers-----
		String postRegisterUrl = "http://localhost:" + port + AuthController.registerUrl;
		String initUsername = "InitUser";
		UserRegistrationDTO userRegistrationDTO = new UserRegistrationDTO(initUsername, initEmail, initPassword, initPassword);
		RestAssured.given()
				.body(userRegistrationDTO).headers(headers)
				.when()
				.post(postRegisterUrl)
				.then().statusCode(HttpStatus.CREATED.value());
		userId = findUserByEmail().map(User::getId).orElseThrow();
	}

	@AfterEach
	void cleanupAfterEach() {
		findRefTokenByEmail().ifPresent(refreshTokenRepository::delete);
		findUserByEmail().ifPresent(userRepository::delete);
	}


	@Nested
	@DisplayName("/delete/{id}")
	class Delete {
		private String deleteUserByIdUrl;

		@BeforeEach
		void initEach() {
			deleteUserByIdUrl = "http://localhost:" + port + UserController.deleteUserByIdUrl;
		}

		@Test
		@DisplayName("✅ Happy Case: Delete user with valid id and password")
		void givenValidData_WhenDeleting_ThenReturnsOk() {
			UserDeleteByIdDTO body = new UserDeleteByIdDTO(userId, initPassword);
			RestAssured.given()
					.headers(headers)
					.body(body)
					.when()
					.delete(deleteUserByIdUrl)
					.then()
					.statusCode(HttpStatus.OK.value())
					.body("success", Matchers.equalTo(true))
					.body("message", Matchers.equalTo(Messages.USER_DELETED));
			assertFalse(userRepository.findById(userId).isPresent());
		}

		@Test
		@DisplayName("❌ Negative Case: Should reject deletion when password is wrong")
		void givenWrongPassword_WhenDeleting_ThenUnauthorized() {
			UserDeleteByIdDTO body = new UserDeleteByIdDTO(userId, "wrongPass123");
			RestAssured.given()
					.headers(headers)
					.body(body)
					.when()
					.delete(deleteUserByIdUrl)
					.then()
					.statusCode(HttpStatus.UNAUTHORIZED.value());
		}

		@Test
		@DisplayName("❌ Negative Case: Should return NOT_FOUND when user does not exist")
		void givenInvalidId_WhenDeleting_ThenNotFound() {
			UserDeleteByIdDTO body = new UserDeleteByIdDTO(UUID.randomUUID(), initPassword);
			RestAssured.given()
					.headers(headers)
					.body(body)
					.when()
					.delete(deleteUserByIdUrl)
					.then()
					.statusCode(HttpStatus.NOT_FOUND.value());
		}

		@Test
		@DisplayName("❌ Negative Case: Should return BAD_REQUEST when incomplete body")
		void givenMissingFields_WhenDeleting_ThenBadRequest() {
			Map<String, Object> body = Map.of(
					"userId", userId
			);
			RestAssured.given()
					.headers(headers)
					.body(body)
					.when()
					.delete(deleteUserByIdUrl)
					.then()
					.statusCode(HttpStatus.BAD_REQUEST.value());
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
