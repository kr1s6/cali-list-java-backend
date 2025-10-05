package com.CalisthenicList.CaliList.service.tokens;
import com.CalisthenicList.CaliList.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessTokenServiceTest {
	@Mock
	private JwtUtils jwtUtils;
	@InjectMocks
	private AccessTokenService accessTokenService;
	private final int durationOfMinutes = 30;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(accessTokenService, "accessTokenDuration", durationOfMinutes);
	}

	private String generateAccessToken(String email) {
		return accessTokenService.generateAccessToken(email);
	}

	@Test
	@DisplayName("✅ Happy Case: Create valid access token")
	void givenEmail_whenGenerateAccessToken_thenDelegatesToJwtUtils() {
		// Given
		String email = "test@example.com";
		String expectedToken = "header.payload.signature";
		Duration expectedDuration = Duration.ofMinutes(durationOfMinutes);
		when(jwtUtils.generateJwt(eq(email), eq(expectedDuration)))
				.thenReturn(expectedToken);
		// When
		String token = generateAccessToken(email);
		// Then
		assertEquals(expectedToken, token, "Access token should match the mocked JWT");
		verify(jwtUtils, times(1)).generateJwt(eq(email), eq(expectedDuration));
	}

	@Test
	@DisplayName("❌ Negative Case: Should throw when JwtUtils fails")
	void givenJwtUtilsThrows_whenGenerateAccessToken_thenPropagateException() {
		// Given
		String email = "fail@example.com";
		when(jwtUtils.generateJwt(anyString(), any()))
				.thenThrow(new IllegalStateException("JWT generation failed"));
		// When / Then
		assertThrows(IllegalStateException.class, () -> generateAccessToken(email),
				"Should propagate exception from JwtUtils");
	}
}