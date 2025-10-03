package com.CalisthenicList.CaliList.repositories;

import com.CalisthenicList.CaliList.configurations.JpaAuditingConfiguration;
import com.CalisthenicList.CaliList.model.RefreshToken;
import com.CalisthenicList.CaliList.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaAuditingConfiguration.class)
class RefreshTokenRepositoryTest {
	@Autowired
	private RefreshTokenRepository refreshTokenRepository;
	@Autowired
	private UserRepository userRepository;
	private User user;
	private RefreshToken refreshToken;

	@BeforeEach
	void setUp() {
		user = new User("testUser", "test@example.com", "password");
		user = userRepository.saveAndFlush(user);
		refreshToken = new RefreshToken();
		refreshToken.setUser(user);
		refreshToken.setToken("sample-token-123");
		refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));
		refreshTokenRepository.saveAndFlush(refreshToken);
	}

	private Optional<RefreshToken> findByToken(String token) {
		return refreshTokenRepository.findByToken(token);
	}

	private Optional<RefreshToken> findByUserEmail(String email) {
		return refreshTokenRepository.findByUserEmail(email);
	}

	@Test
	@DisplayName("✅ Should find refresh token by token string")
	void testFindByToken() {
		Optional<RefreshToken> found = findByToken("sample-token-123");
		assertTrue(found.isPresent());
		assertEquals(refreshToken.getToken(), found.get().getToken());
		assertEquals(user.getEmail(), found.get().getUser().getEmail());
	}

	@Test
	@DisplayName("✅ Should return empty if token not found")
	void testFindByTokenNotFound() {
		Optional<RefreshToken> found = findByToken("non-existing-token");
		assertTrue(found.isEmpty());
	}

	@Test
	@DisplayName("✅ Should find refresh token by user email")
	void testFindByUserEmail() {
		Optional<RefreshToken> found = findByUserEmail(user.getEmail());
		assertTrue(found.isPresent());
		assertEquals(refreshToken.getToken(), found.get().getToken());
	}

	@Test
	@DisplayName("✅ Should return empty if user email not found")
	void testFindByUserEmailNotFound() {
		Optional<RefreshToken> found = findByUserEmail("unknown@example.com");
		assertTrue(found.isEmpty());
	}

	@Test
	@DisplayName("✅ Should save and delete refresh token")
	void testSaveAndDelete() {
		//When
		Optional<RefreshToken> saved = refreshTokenRepository.findByToken("sample-token-123");
		//Then
		assertTrue(saved.isPresent());
		refreshTokenRepository.delete(saved.get());
		Optional<RefreshToken> deleted = findByToken("sample-token-123");
		assertTrue(deleted.isEmpty());
	}
}