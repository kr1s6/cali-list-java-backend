package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.constants.UserConstants;
import com.CalisthenicList.CaliList.enums.Roles;
import com.CalisthenicList.CaliList.filter.UserValidationRateLimitingFilter;
import com.CalisthenicList.CaliList.model.*;
import com.CalisthenicList.CaliList.repositories.RefreshTokenRepository;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import com.CalisthenicList.CaliList.service.tokens.AccessTokenService;
import com.CalisthenicList.CaliList.service.tokens.RefreshTokenService;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerTest {
	@LocalServerPort
	private int port;
	private HttpHeaders headers;
	private UserRegistrationDTO userRegistrationDTO;
	private UserLoginDTO userLoginDTO;
	private String postRegisterUrl;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	@Autowired
	private UserValidationRateLimitingFilter filter;
	@Autowired
	private RefreshTokenService refreshTokenService;
	@Autowired
	private AccessTokenService accessTokenService;
	private int maxRequestsPerMinute;
	private final String initUsername = "InitUser";
	private final String initEmail = "Inittest@interia.pl";
	private final String initPassword = "Init password test";

	@Autowired
	public AuthControllerTest(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
	}

	private Optional<User> findUserByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	private Optional<User> findUserByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	private Optional<RefreshToken> findRefTokenByEmail(String email) {
		return refreshTokenRepository.findByUserEmail(email);
	}

	@BeforeAll
	void initBeforeAll() {
		//-----Headers-----
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Forwarded-For", "10.0.0.7");
		//-----Headers-----
		postRegisterUrl = "http://localhost:" + port + AuthController.registerUrl;
		userRegistrationDTO = new UserRegistrationDTO(initUsername, initEmail, initPassword, initPassword);
		RestAssured.given()
				.body(userRegistrationDTO).headers(headers)
				.when()
				.post(postRegisterUrl)
				.then().statusCode(HttpStatus.CREATED.value());
	}

	@AfterAll
	void cleanupAfterAll() {
		findRefTokenByEmail(initEmail).ifPresent(refreshTokenRepository::delete);
		findUserByEmail(initEmail).ifPresent(userRepository::delete);
	}

	@Nested
	@DisplayName("/register")
	class Register {
		private final String validUsername = "TestUser";
		private final String validEmail = "test@interia.pl";

		@BeforeEach
		void initEach() {
			postRegisterUrl = "http://localhost:" + port + AuthController.registerUrl;
			String validPassword = "qWBRę LGć8MPł test";
			if(findUserByEmail(validEmail).isPresent()) {
				findRefTokenByEmail(validEmail).ifPresent(refreshTokenRepository::delete);
				findUserByEmail(validEmail).ifPresent(userRepository::delete);
			}
			userRegistrationDTO = new UserRegistrationDTO(validUsername, validEmail, validPassword, validPassword);
		}

		@AfterEach
		void cleanUp() {
			findRefTokenByEmail(validEmail).ifPresent(refreshTokenRepository::delete);
			findUserByEmail(validEmail).ifPresent(userRepository::delete);
		}

		@Test
		@DisplayName("✅ Happy Case: User created with valid credentials")
		void givenValidValues_whenSendingPostRegister_thenReturnsCreatedSuccessfully() {
			RestAssured.given()
					.body(userRegistrationDTO).headers(headers).log().all()
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
			assertTrue(findUserByUsername(validUsername).isPresent(), "Warning! Username not found in DB");
			assertTrue(findUserByEmail(validEmail).isPresent(), "Warning! Email not found in DB");
		}

		@Test
		@DisplayName("✅ Happy Case: Max length password")
		void givenMaxLengthPassword_whenSendingPostRegister_thenReturnsCreatedSuccessfully() {
			String veryLongPassword = "a".repeat(UserConstants.PASSWORD_MAX_LENGTH);
			userRegistrationDTO.setPassword(veryLongPassword);
			userRegistrationDTO.setConfirmPassword(veryLongPassword);
			RestAssured.given()
					.body(userRegistrationDTO).headers(headers).log().all()
					.when()
					.post(postRegisterUrl)
					.then()
					.log().all()
					.statusCode(HttpStatus.CREATED.value());
			assertTrue(findUserByEmail(validEmail).isPresent(), "Warning! Email not found in DB");
		}

		@Test
		@DisplayName("❌ Negative Case: Too long password")
		void givenTooLongPassword_whenSendingPostRegister_thenReturnsBadRequestError() {
			String tooLongPassword = "a".repeat(UserConstants.PASSWORD_MAX_LENGTH + 1);
			userRegistrationDTO.setPassword(tooLongPassword);
			userRegistrationDTO.setConfirmPassword(tooLongPassword);
			RestAssured.given()
					.body(userRegistrationDTO).headers(headers).log().all()
					.when()
					.post(postRegisterUrl)
					.then()
					.log().all()
					.statusCode(HttpStatus.BAD_REQUEST.value())
					.body("success", Matchers.equalTo(false))
					.body("accessToken", Matchers.nullValue())
					.body("message", Matchers.equalTo(Messages.VALIDATION_FAILED));
			assertFalse(findUserByUsername(validUsername).isPresent(), "Warning! Username not found in DB");
			assertFalse(findUserByEmail(validEmail).isPresent(), "Warning! Email not found in DB");
		}

		//INFO - Scenario where user is registering with valid access token is not important, cause it doesn't change anything
		//INFO - Invalid username scenario is tested
		//INFO - Invalid email scenario is tested
		//INFO - Invalid password scenario is tested
	}

	@Nested
	@DisplayName("/email-verification/{token}")
	class EmailVerification {
		//	INFO - check manually
	}

	@Nested
	@DisplayName("/login")
	class Login {
		private String postLoginUrl;

		@BeforeEach
		void initEach() {
			filter = new UserValidationRateLimitingFilter();
			maxRequestsPerMinute = filter.MAX_HEAVY_REQUESTS_PER_MINUTE;
			postLoginUrl = "http://localhost:" + port + AuthController.loginUrl;
			userLoginDTO = new UserLoginDTO(initEmail, initPassword);
		}

		@Test
		@DisplayName("✅ Happy Case: Login user")
		void givenValidValues_WhenSendingLoginRequest_ThenReturnsOk() {
			RestAssured.given()
					.body(userLoginDTO).headers(headers).log().all()
					.when()
					.post(postLoginUrl)
					.then()
					.log().all()
					.statusCode(HttpStatus.OK.value())
					.body("success", Matchers.equalTo(true))
					.body("message", Matchers.equalTo(Messages.LOGIN_SUCCESS))
					.body("accessToken", Matchers.notNullValue())
					.body("data.id", Matchers.notNullValue())
					.body("data.role", Matchers.equalTo(Roles.ROLE_USER.name()))
					.body("data.username", Matchers.equalTo(initUsername))
					.body("data.email", Matchers.equalTo(initEmail))
					.body("data.emailVerified", Matchers.equalTo(false))
					.body("data.birthDate", Matchers.nullValue())
					.body("data.userData.caliStartDate", Matchers.nullValue());
		}

		@Test
		@DisplayName("❌ Negative Case: Login with bad password")
		void givenInvalidPassword_WhenSendingLoginRequest_ThenReturnsStatusConflict() {
			userLoginDTO = new UserLoginDTO(initEmail, "invalidPassword");
			RestAssured.given()
					.body(userLoginDTO).headers(headers).log().all()
					.when()
					.post(postLoginUrl)
					.then()
					.log().all()
					.statusCode(HttpStatus.UNAUTHORIZED.value())
					.body("success", Matchers.equalTo(false))
					.body("message", Matchers.equalTo(Messages.INVALID_LOGIN_ERROR))
					.body("accessToken", Matchers.nullValue())
					.body("data", Matchers.equalTo(Messages.INVALID_LOGIN_ERROR));
		}

		@Test
		@DisplayName("❌ Negative Case: Login with bad email")
		void givenInvalidEmail_WhenSendingLoginRequest_ThenReturnsStatusNotFound() {
			userLoginDTO = new UserLoginDTO("invalid@interia.pl", initPassword);
			RestAssured.given()
					.body(userLoginDTO).headers(headers).log().all()
					.when()
					.post(postLoginUrl)
					.then()
					.log().all()
					.statusCode(HttpStatus.NOT_FOUND.value())
					.body("success", Matchers.equalTo(false))
					.body("message", Matchers.equalTo(Messages.INVALID_LOGIN_ERROR))
					.body("accessToken", Matchers.nullValue())
					.body("data", Matchers.equalTo(Messages.INVALID_LOGIN_ERROR));
		}

		@Test
		@DisplayName("❌ Negative Case: Block coming 'Post Register' requests after reaching requests limit.")
		void givenRequestsOverLimit_whenSendingPostRegister_thenReturnsTooManyRequestsError() throws InterruptedException {
			Thread.sleep(filter.REFILL_PERIOD);
			for(int i = 0; i < maxRequestsPerMinute; i++) {
				RestAssured.given()
						.body(userLoginDTO).headers(headers)
						.when().post(postLoginUrl)
						.then().statusCode(HttpStatus.OK.value());
			}
			RestAssured.given()
					.body(userLoginDTO).headers(headers)
					.when().post(postLoginUrl)
					.then()
					.log().all()
					.statusCode(HttpStatus.TOO_MANY_REQUESTS.value());
		}
	}

	@Nested
	@DisplayName("/refreshToken")
	class RefreshAccessToken {
		private String postRefreshTokenUrl;

		@BeforeEach
		void initEach() {
			postRefreshTokenUrl = "http://localhost:" + port + AuthController.refreshTokenUrl;
		}

		@Test
		@DisplayName("✅ Happy Case: 200 + accessToken + new cookie")
		void givenValidRefreshToken_WhenRefreshing_ThenReturnsNewTokens() {
			ResponseCookie response = refreshTokenService.createCookieWithRefreshToken(initEmail);
			RestAssured.given()
					.cookie("refreshToken", response.getValue())
					.when()
					.post(postRefreshTokenUrl)
					.then()
					.statusCode(HttpStatus.OK.value())
					.body("success", Matchers.equalTo(true))
					.body("message", Matchers.equalTo(Messages.REFRESH_TOKEN_SUCCESS))
					.body("accessToken", Matchers.notNullValue())
					.header(HttpHeaders.SET_COOKIE, Matchers.containsString("refreshToken="));
		}

		@Test
		@DisplayName("❌ Negative Case: Refresh token is null")
		void givenNoCookie_WhenRefreshing_ThenUnauthorized() {
			RestAssured.given()
					.when()
					.post(postRefreshTokenUrl)
					.then()
					.statusCode(HttpStatus.UNAUTHORIZED.value())
					.body("success", Matchers.equalTo(false))
					.body("message", Matchers.equalTo("Unauthorized - no refresh token"));
		}

		@Test
		@DisplayName("❌ Negative Case: Refresh token don't exists in db")
		void givenInvalidCookie_WhenRefreshing_ThenUnauthorized() {
			RestAssured.given()
					.cookie("refreshToken", "fake-token-123")
					.when()
					.post(postRefreshTokenUrl)
					.then()
					.statusCode(HttpStatus.NOT_FOUND.value());
		}
	}

	@Nested
	@DisplayName("/passwordRecovery/{token}")
	class PasswordRecoveryTest {
		private String passwordRecoveryUrl;

		@BeforeEach
		void initEach() {
			passwordRecoveryUrl = "http://localhost:" + port + AuthController.passwordRecoveryUrl;
		}

		@Test
		@DisplayName("✅ Happy Case: valid token + valid passwords → 200 OK")
		void givenValidRequest_WhenPasswordRecovery_ThenSuccess() {
			String jwt = accessTokenService.generateAccessToken(initEmail);
			PasswordRecoveryDTO dto = new PasswordRecoveryDTO();
			dto.setPassword("NewPassword123!");
			dto.setConfirmPassword("NewPassword123!");
			RestAssured.given()
					.body(dto)
					.headers(headers)
					.when()
					.post(passwordRecoveryUrl.replace("{token}", jwt))
					.then()
					.statusCode(HttpStatus.OK.value())
					.body("success", Matchers.equalTo(true))
					.body("message", Matchers.equalTo("Password recovered successfully."));
		}

		@Test
		@DisplayName("❌ Error: passwords do not match → 400 Bad Request")
		void givenDifferentPasswords_WhenPasswordRecovery_ThenBadRequest() {
			String jwt = accessTokenService.generateAccessToken(initEmail);
			PasswordRecoveryDTO dto = new PasswordRecoveryDTO();
			dto.setPassword("Password123!");
			dto.setConfirmPassword("WrongPassword123");
			RestAssured.given()
					.body(dto)
					.headers(headers)
					.when()
					.post(passwordRecoveryUrl.replace("{token}", jwt))
					.then()
					.statusCode(HttpStatus.UNAUTHORIZED.value())
					.body("success", Matchers.equalTo(false));
		}

		@Test
		@DisplayName("❌ Error: user not found → 404")
		void givenUnknownUser_WhenPasswordRecovery_ThenUserNotFound() {
			String unknownEmail = "unknown@example.com";
			String jwt = accessTokenService.generateAccessToken(unknownEmail);
			PasswordRecoveryDTO dto = new PasswordRecoveryDTO();
			dto.setPassword("Password123!");
			dto.setConfirmPassword("Password123!");
			RestAssured.given()
					.body(dto)
					.headers(headers)
					.when()
					.post(passwordRecoveryUrl.replace("{token}", jwt))
					.then()
					.statusCode(HttpStatus.NOT_FOUND.value())
					.body("success", Matchers.equalTo(false));
		}

		@Test
		@DisplayName("❌ Error: invalid token → 400")
		void givenInvalidToken_WhenPasswordRecovery_ThenBadRequest() {
			String jwt = "invalid.jwt.token";
			PasswordRecoveryDTO dto = new PasswordRecoveryDTO();
			dto.setPassword("Password123!");
			dto.setConfirmPassword("Password123!");
			RestAssured.given()
					.body(dto)
					.headers(headers)
					.when()
					.post(passwordRecoveryUrl.replace("{token}", jwt))
					.then()
					.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
	}

}
