package com.CalisthenicList.CaliList.filter;

import com.CalisthenicList.CaliList.configurations.SecurityConfig;
import com.CalisthenicList.CaliList.model.UserRegistrationDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RateLimitingFilterTest {
	private static final int REQUESTS_PER_MINUTE = 5;
	private static HttpHeaders headers;
	private static UserRegistrationDTO userRegistrationDTO;
	@Autowired
	private TestRestTemplate testRestTemplate;
	@LocalServerPort
	private int port;
	private String postRegisterUrl;

	@BeforeAll
	static void initAll() {
		SecurityConfig.TOKEN_CAPACITY = REQUESTS_PER_MINUTE;
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
	}

	@BeforeEach
	void initEach() {
		String validUsername = "TestUser";
		String validEmail = "test@intera.pl";
		String invalidPassword = "Invalid";
		postRegisterUrl = "http://localhost:" + port + "/api/user/register";
		userRegistrationDTO = new UserRegistrationDTO(validUsername, validEmail, invalidPassword, invalidPassword);
	}

	@Test
	@DisplayName("❌ Negative Case: After sending 5 http requests during 1 min, user can't send another request during the minute.")
	void givenFiveRequestsDuringOneMin_WhenSendingSixthRequest_ThenRateLimitingFilterRejectsRequest() {
		// Given
		HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
		// When
		ResponseEntity<String> response;
		for(int i = 0; i < REQUESTS_PER_MINUTE; i++) {
			response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
			assertTrue(response.getStatusCode().is4xxClientError(),
					"Warning! Registration passed with too short password. Code: " + response.getStatusCode());
		}
		response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
		// Then
		assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS),
				"Warning! Too many requests passed for user during 1 minute. Code: " + response.getStatusCode());
	}

	@Test
	@DisplayName("✅ Happy Case: User after using all request in bucket, can send another 5 request after minute.")
	void givenFiveRequestsDuringOneMin_WhenWaitingOneMinute_ThenUserCanSendAnotherFiveRequest() throws InterruptedException {
		// Given
		HttpEntity<UserRegistrationDTO> registrationRequest = new HttpEntity<>(userRegistrationDTO, headers);
		ResponseEntity<String> response;
		for(int i = 0; i < REQUESTS_PER_MINUTE; i++) {
			response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST),
					"Warning! Wrong response. Code: " + response.getStatusCode());
		}
		// When
		Thread.sleep(Duration.ofSeconds(60));
		// Then
		for(int i = 0; i < REQUESTS_PER_MINUTE; i++) {
			response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST),
					"Warning! Wrong Code: " + response.getStatusCode());
		}
	}

	// java
	@Test
	@DisplayName("Happy Case: - User2 unaffected when User1 hits the limit.")
	void givenSendFiveRequestForUser1_whenSendingRequestFromUser2_thenUser2IsNotRateLimited() {
		// Given
		String ip1 = "10.0.0.1";
		String ip2 = "10.0.0.2";
		HttpHeaders h1 = new HttpHeaders();
		HttpHeaders h2 = new HttpHeaders();
		h1.setContentType(MediaType.APPLICATION_JSON);
		h2.setContentType(MediaType.APPLICATION_JSON);
		h1.set("X-Forwarded-For", ip1);
		h2.set("X-Forwarded-For", ip2);
		HttpEntity<UserRegistrationDTO> user1RegistrationRequest = new HttpEntity<>(userRegistrationDTO, h1);
		HttpEntity<UserRegistrationDTO> user2RegistrationRequest = new HttpEntity<>(userRegistrationDTO, h2);
		// When
		ResponseEntity<String> response;
		for(int i = 0; i < REQUESTS_PER_MINUTE; i++) {
			response = testRestTemplate.postForEntity(postRegisterUrl, user1RegistrationRequest, String.class);
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST),
					"Warning! Wrong response. Code: " + response.getStatusCode());
		}
		// Then
		for(int i = 0; i < REQUESTS_PER_MINUTE; i++) {
			response = testRestTemplate.postForEntity(postRegisterUrl, user2RegistrationRequest, String.class);
			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST),
					"User2's bucket shouldn't be shared with User1. Code: " + response.getStatusCode());
		}
	}
}
