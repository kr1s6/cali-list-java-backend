package com.CalisthenicList.CaliList.service.tokens;
import com.CalisthenicList.CaliList.model.RefreshToken;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.repositories.RefreshTokenRepository;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import com.CalisthenicList.CaliList.utils.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
	@Mock
	private AccessTokenService accessTokenService;
	@Mock
	private JwtUtils jwtUtils;
	@Mock
	private RefreshTokenRepository refreshTokenRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private HttpServletResponse httpResponse;
	@InjectMocks
	private RefreshTokenService refreshTokenService;
	private final Instant duration = Instant.now().plus(Duration.ofDays(30));
	private User user;
	private RefreshToken refreshToken;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDuration", 30);
		refreshTokenService.init();
		user = new User("testuser", "test@example.com", "pass");
		refreshToken = new RefreshToken(UUID.randomUUID(), user, "refresh.jwt.token", duration);
	}

	@Nested
	@DisplayName("createCookieWithRefreshToken")
	class CreateCookieWithRefreshTokenTest {
		@Test
		@DisplayName("✅ Should create cookie with refresh token for given user")
		void givenUser_whenCreateCookieWithRefreshToken_thenReturnCookie() {
			// Given
			String jwt = "jwt.token";
			when(jwtUtils.generateJwt(eq(user.getEmail()), any())).thenReturn(jwt);
			when(refreshTokenRepository.findByUserEmail(user.getEmail())).thenReturn(Optional.empty());
			when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
			// When
			ResponseCookie cookie = refreshTokenService.createCookieWithRefreshToken(user.getEmail(), user);
			// Then
			assertEquals("refreshToken", cookie.getName());
			assertEquals(jwt, cookie.getValue());
			assertTrue(cookie.isHttpOnly());
			assertTrue(cookie.isSecure());
			assertEquals("/", cookie.getPath());
		}
	}

	@Nested
	@DisplayName("createRefreshToken")
	class CreateRefreshTokenTest {

		private RefreshToken createRefreshToken(String email, User user) {
			return refreshTokenService.createRefreshToken(email, user);
		}

		@Test
		@DisplayName("✅ Should create new refresh token if none exists")
		void givenNoExistingToken_whenCreateRefreshToken_thenNewTokenSaved() {
			// Given
			String jwt = "new.jwt.token";
			when(jwtUtils.generateJwt(eq(user.getEmail()), any())).thenReturn(jwt);
			when(refreshTokenRepository.findByUserEmail(user.getEmail())).thenReturn(Optional.empty());
			when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
			// When
			RefreshToken token = createRefreshToken(user.getEmail(), user);
			// Then
			assertEquals(jwt, token.getToken());
			assertEquals(user, token.getUser());
			assertTrue(token.getExpiryDate().isAfter(Instant.now()));
			verify(refreshTokenRepository).save(token);
		}

		@Test
		@DisplayName("✅ Should update existing refresh token")
		void givenExistingToken_whenCreateRefreshToken_thenUpdateAndSave() {
			// Given
			when(refreshTokenRepository.findByUserEmail(user.getEmail())).thenReturn(Optional.of(refreshToken));
			when(jwtUtils.generateJwt(eq(user.getEmail()), any())).thenReturn("updated.jwt.token");
			when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
			// When
			RefreshToken updated = createRefreshToken(user.getEmail(), user);
			// Then
			assertEquals("updated.jwt.token", updated.getToken());
			assertTrue(updated.getExpiryDate().isAfter(Instant.now()));
			verify(refreshTokenRepository).save(refreshToken);
		}
	}

	@Nested
	@DisplayName("refreshAccessToken")
	class RefreshAccessTokenTest {

		private ResponseEntity<?> refreshAccessToken(String token, HttpServletResponse response) {
			return refreshTokenService.refreshAccessToken(token, response);
		}

		@Test
		@DisplayName("❌ Should return UNAUTHORIZED if token is null")
		void givenNullToken_whenRefreshAccessToken_thenUnauthorized() {
			ResponseEntity<?> response = refreshAccessToken(null, httpResponse);
			assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		}

		@Test
		@DisplayName("❌ Should return UNAUTHORIZED if token not found in DB")
		void givenNonexistentToken_whenRefreshAccessToken_thenUnauthorized() {
			when(refreshTokenRepository.findByToken("bad.token")).thenReturn(Optional.empty());
			assertThrows(UsernameNotFoundException.class, () -> refreshAccessToken("bad.token", httpResponse));
		}

		@Test
		@DisplayName("❌ Should return UNAUTHORIZED if JWT subject is invalid")
		void givenInvalidJwtSubject_whenRefreshAccessToken_thenUnauthorized() {
			//Given
			when(refreshTokenRepository.findByToken(refreshToken.getToken())).thenReturn(Optional.of(refreshToken));
			when(jwtUtils.extractSubject(refreshToken.getToken())).thenReturn("other@example.com");
			when(jwtUtils.validateIfJwtSubjectMatchTheUser("other@example.com", user.getEmail())).thenReturn(false);
			//When
			ResponseEntity<?> response = refreshAccessToken(refreshToken.getToken(), httpResponse);
			//Then
			assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		}

		@Test
		@DisplayName("❌ Should delete and return expired token response")
		void givenExpiredRefreshToken_whenRefreshAccessToken_thenDeleteAndBadRequest() {
			//Given
			RefreshToken expired = new RefreshToken(UUID.randomUUID(), user, "expired.jwt",
					Instant.now().minus(Duration.ofDays(1)));
			when(refreshTokenRepository.findByToken(expired.getToken())).thenReturn(Optional.of(expired));
			when(jwtUtils.extractSubject(expired.getToken())).thenReturn(user.getEmail());
			when(jwtUtils.validateIfJwtSubjectMatchTheUser(user.getEmail(), user.getEmail())).thenReturn(true);
			//When
			ResponseEntity<?> response = refreshAccessToken(expired.getToken(), httpResponse);
			//Then
			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
			verify(refreshTokenRepository).delete(expired);
		}

		@Test
		@DisplayName("✅ Should refresh access token when valid")
		void givenValidToken_whenRefreshAccessToken_thenReturnNewAccessToken() {
			//Given
			when(refreshTokenRepository.findByToken(refreshToken.getToken())).thenReturn(Optional.of(refreshToken));
			when(jwtUtils.extractSubject(refreshToken.getToken())).thenReturn(user.getEmail());
			when(jwtUtils.validateIfJwtSubjectMatchTheUser(user.getEmail(), user.getEmail())).thenReturn(true);
			when(accessTokenService.generateAccessToken(user.getEmail())).thenReturn("new.access.token");
			when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
			when(jwtUtils.generateJwt(any(), any())).thenReturn("new.refresh.jwt");
			when(refreshTokenRepository.findByUserEmail(user.getEmail())).thenReturn(Optional.of(refreshToken));
			when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
			//When
			ResponseEntity<?> response = refreshAccessToken(refreshToken.getToken(), httpResponse);
			//Then
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			Map<String, String> body = (Map<String, String>) response.getBody();
			assertEquals("new.access.token", body.get("accessToken"));
			verify(httpResponse).addHeader(eq(HttpHeaders.SET_COOKIE), contains("refreshToken="));
		}
	}

	@Nested
	@DisplayName("deleteRefreshToken")
	class DeleteRefreshTokenTest {

		private ResponseEntity<?> deleteRefreshToken(String token, HttpServletResponse response) {
			return refreshTokenService.deleteRefreshToken(token, response);
		}

		@Test
		@DisplayName("❌ Should return BadRequest when deleting with null token")
		void givenNullToken_whenDeleteRefreshToken_thenBadRequest() {
			ResponseEntity<?> response = deleteRefreshToken(null, httpResponse);
			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		}

		@Test
		@DisplayName("✅ Should delete token and set expired cookie")
		void givenValidToken_whenDeleteRefreshToken_thenDeleteAndReturnOk() {
			//Given
			when(refreshTokenRepository.findByToken(refreshToken.getToken())).thenReturn(Optional.of(refreshToken));
			//When
			ResponseEntity<?> response = deleteRefreshToken(refreshToken.getToken(), httpResponse);
			//Then
			assertEquals(HttpStatus.OK, response.getStatusCode());
			verify(refreshTokenRepository).delete(refreshToken);
			verify(httpResponse).addHeader(eq(HttpHeaders.SET_COOKIE), contains("Max-Age=0"));
		}
	}

	@Nested
	@DisplayName("isRefreshTokenExpired")
	class IsRefreshTokenExpiredTest {

		private boolean isRefreshTokenExpired(RefreshToken token) {
			return refreshTokenService.isRefreshTokenExpired(token);
		}

		@Test
		@DisplayName("✅ Should detect expired refresh token")
		void givenExpiredToken_whenCheckIsExpired_thenTrue() {
			refreshToken.setExpiryDate(Instant.now().minusSeconds(10));
			assertTrue(isRefreshTokenExpired(refreshToken));
		}

		@Test
		@DisplayName("✅ Should detect valid refresh token")
		void givenValidToken_whenCheckIsExpired_thenFalse() {
			refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));
			assertFalse(isRefreshTokenExpired(refreshToken));
		}
	}
}