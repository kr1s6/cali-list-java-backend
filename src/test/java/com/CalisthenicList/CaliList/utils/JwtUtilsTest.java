package com.CalisthenicList.CaliList.utils;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {
	private JwtUtils jwtUtils;

	@BeforeEach
	void setUp() {
		jwtUtils = new JwtUtils();
		ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "12345678901234567890123456789012");
		jwtUtils.init();
	}

	@Test
	@DisplayName("✅ generateJwt and extractSubject should work correctly")
	void generateJwt_and_extractSubject() {
		// Given
		String email = "test@example.com";
		Duration duration = Duration.ofMinutes(5);
		// When
		String token = jwtUtils.generateJwt(email, duration);
		String subject = jwtUtils.extractSubject(token);
		// Then
		assertNotNull(token);
		assertEquals(email, subject);
	}

	@Test
	@DisplayName("✅ validateIfJwtSubjectMatchTheUser returns true when subject matches email")
	void validateIfJwtSubjectMatchTheUser_match() {
		assertTrue(jwtUtils.validateIfJwtSubjectMatchTheUser("test@example.com", "test@example.com"));
	}

	@Test
	@DisplayName("❌ validateIfJwtSubjectMatchTheUser returns false when subject does not match email")
	void validateIfJwtSubjectMatchTheUser_noMatch() {
		assertFalse(jwtUtils.validateIfJwtSubjectMatchTheUser("test@example.com", "other@example.com"));
	}

	@Test
	@DisplayName("❌ extractClaims throws ExpiredJwtException when token is expired")
	void extractClaims_expiredJwt() {
		// Given:
		String email = "expired@example.com";
		String token = jwtUtils.generateJwt(email, Duration.ofMillis(1));
		// Sleep
		try {
			Thread.sleep(5);
		} catch(InterruptedException ignored) {
		}
		// When + Then
		assertThrows(ExpiredJwtException.class, () -> jwtUtils.extractSubject(token));
	}

	@Test
	@DisplayName("❌ extractClaims throws SignatureException for invalid token")
	void extractClaims_invalidSignature() {
		String email = "invalid@example.com";
		String token = jwtUtils.generateJwt(email, Duration.ofMinutes(1));
		String badToken = token.substring(0, token.length() - 2) + "xx";
		assertThrows(SignatureException.class, () -> jwtUtils.extractSubject(badToken));
	}
}
