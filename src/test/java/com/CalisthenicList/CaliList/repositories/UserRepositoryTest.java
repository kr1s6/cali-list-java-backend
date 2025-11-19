package com.CalisthenicList.CaliList.repositories;

import com.CalisthenicList.CaliList.configurations.JpaAuditingConfiguration;
import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.constants.UserConstants;
import com.CalisthenicList.CaliList.enums.Roles;
import com.CalisthenicList.CaliList.model.User;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaAuditingConfiguration.class)
//INFO: Configures an in-memory database (H2) by default,
// - starts Spring, but only the JPA layer (not controllers, not services),
// - rolls back the transaction after each test (tests remain clean).
class UserRepositoryTest {
	private final String validUsername = "TestUser";
	private final String validEmail = "test@intera.pl";
	private final String validPassword = "qWBRę LGć8MPł test";
	@Autowired
	private UserRepository userRepository;

	private Optional<User> findByEmail(String email){
		return userRepository.findByEmail(email);
	}

	private Optional<User> findByUsername(String username){
		return userRepository.findByUsername(username);
	}

	@Test
	@DisplayName("✅ Happy Case: Role is set to ROLE_USER")
	void givenUser_whenSave_thenRoleIsUser() {
		// Given
		User user = new User(validUsername, validEmail, validPassword);
		// When
		user = userRepository.saveAndFlush(user);
		// Then
		assertEquals(Roles.ROLE_USER, user.getRole());
	}

	@Test
	@DisplayName("✅ Happy Case: Email verification is set to false at the start")
	void givenUser_whenSave_thenEmailVerificationIsFalse() {
		// Given
		User user = new User(validUsername, validEmail, validPassword);
		// When
		user = userRepository.saveAndFlush(user);
		// Then
		assertFalse(user.isEmailVerified());
	}

	@Test
	@DisplayName("✅ Happy Case: createdDate and updatedDate is set when user is created")
	void givenNewUser_whenSave_thenCreatedDateIsSet() {
		// Given
		User user = new User(validUsername, validEmail, validPassword);
		// When
		User savedUser = userRepository.saveAndFlush(user);
		// Then
		assertNotNull(savedUser.getCreatedDate(), "createdDate should be set after persisting");
		assertNotNull(savedUser.getUpdatedDate(), "updatedDate should be set after persisting");
		assertEquals(savedUser.getCreatedDate(), savedUser.getUpdatedDate(),
				"On insert createdDate and updatedDate should be equal");
	}

	@Test
	@DisplayName("✅ Happy Case: updatedDate changes while createdDate stays the same")
	void givenUserUpdated_thenTimestampsBehaveCorrectly() throws InterruptedException {
		// Given
		User user = new User(validUsername, validEmail, validPassword);
		userRepository.saveAndFlush(user);
		Instant createdAt = user.getCreatedDate();
		Instant updatedAt = user.getUpdatedDate();
		Thread.sleep(5);
		// When
		user.setEmailVerified(true);
		userRepository.saveAndFlush(user);
		// Then
		assertEquals(createdAt, user.getCreatedDate(), "createdDate should stay unchanged");
		assertTrue(user.getUpdatedDate().isAfter(updatedAt), "updatedDate should be updated");
	}


	@Nested
	@DisplayName("Repository tests")
	class RepositoryTests {
		private User user;

		@BeforeEach
		void initEach() {
			// Given
			user = userRepository.save(new User(validUsername, validEmail, validPassword));
		}

		@Test
		@DisplayName("✅ Happy Case: Find user by ID")
		void givenSavedUser_whenFindById_thenReturnUser() {
			// When
			Optional<User> userFound = userRepository.findById(user.getId());
			// Then
			assertTrue(userFound.isPresent(), "User should be found by ID");
			assertEquals(validUsername, userFound.get().getUsername());
		}

		@Test
		@DisplayName("✅ Happy Case: Find user by username")
		void givenExistingUser_whenFindByUsername_thenReturnUser() {
			// When
			Optional<User> userFound = findByUsername(validUsername);
			// Then
			assertTrue(userFound.isPresent(), "User should be found by username");
			assertEquals(validEmail, userFound.get().getEmail(), "Email does not match");
		}

		@Test
		@DisplayName("✅ Happy Case: Find user by email")
		void givenExistingUser_whenFindByEmail_thenReturnUser() {
			// When
			Optional<User> userFound = findByEmail(validEmail);
			// Then
			assertTrue(userFound.isPresent(), "User should be found by email");
			assertEquals(validUsername, userFound.get().getUsername(), "Username does not match");
		}

		@Test
		@DisplayName("❌ Negative Case: Return empty when user doesn't exist")
		void givenNonExistingUser_whenFindByUsernameOrEmail_thenReturnEmpty() {
			// When
			Optional<User> byUsername = findByUsername("not_found");
			Optional<User> byEmail = findByEmail("not_found@example.com");
			// Then
			assertTrue(byUsername.isEmpty(), "No user should be found by username");
			assertTrue(byEmail.isEmpty(), "No user should be found by email");
		}

		@Test
		@DisplayName("✅ Happy Case: Delete user by ID")
		void givenUser_whenDeleteById_thenUserIsRemoved() {
			// When
			userRepository.deleteById(user.getId());
			// Then
			assertFalse(userRepository.findById(user.getId()).isPresent(), "User should be deleted by ID");
		}
	}

	@Nested
	@DisplayName("Username constrains")
	class UsernameConstrainsTests {

		@Test
		@DisplayName("❌ Negative Case: Duplicate username should throw exception")
		void givenDuplicateUsername_whenSave_thenThrowException() {
			// Given
			User user1 = new User(validUsername, "user1@example.com", validPassword);
			User user2 = new User(validUsername, "user2@example.com", validPassword);
			userRepository.saveAndFlush(user1);
			// When Then
			assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user2),
					"Saving duplicate username should fail");
		}

		@DisplayName("❌ Negative Case: Username cannot be blank")
		@ParameterizedTest(name = "Invalid username case: \"{0}\"")
		@NullAndEmptySource
		@ValueSource(strings = {"        "})
		void givenNullUsername_whenSave_thenThrowException(String invalidUsername) {
			// Given
			User user = new User(invalidUsername, validEmail, validPassword);
			// When Then
			ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> userRepository.saveAndFlush(user));
			String message = exception.getConstraintViolations().iterator().next().getMessage();
			assertEquals(Messages.USERNAME_NOT_BLANK_ERROR, message, "Wrong error message.");
		}

		@Test
		@DisplayName("❌ Negative Case: Username too long")
		void givenTooLongUsername_whenSave_thenThrowException() {
			// Given
			String tooLongUsername = "a".repeat(UserConstants.USERNAME_MAX_LENGTH + 1);
			User user = new User(tooLongUsername, validEmail, validPassword);
			// When Then
			ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> userRepository.saveAndFlush(user));
			String message = exception.getConstraintViolations().iterator().next().getMessage();
			// Then
			assertEquals(Messages.USERNAME_LENGTH_ERROR, message, "Wrong error message for long username.");
		}
	}

	@Nested
	@DisplayName("Email constrains")
	class EmailConstrainsTests {

		@Test
		@DisplayName("❌ Negative Case: Duplicate email should throw exception")
		void givenDuplicateEmail_whenSave_thenThrowException() {
			// Given
			User user1 = new User("user1", validEmail, validPassword);
			User user2 = new User("user2", validEmail, validPassword);
			userRepository.saveAndFlush(user1);
			// When Then
			assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user2),
					"Saving duplicate email should fail");
		}

		@DisplayName("❌ Negative Case: Email cannot be blank")
		@ParameterizedTest(name = "Invalid email case: \"{0}\"")
		@NullAndEmptySource
		@ValueSource(strings = {"        "})
		void givenNullEmail_whenSave_thenThrowException(String invalidEmail) {
			// Given
			User user = new User(validUsername, invalidEmail, validPassword);
			// When Then
			ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> userRepository.saveAndFlush(user));
			String message = exception.getConstraintViolations().iterator().next().getMessage();
			assertEquals(Messages.EMAIL_INVALID_ERROR, message, "Wrong error message.");
		}

		@Test
		@DisplayName("❌ Negative Case: Email invalid format")
		void givenNullEmail_whenSave_thenThrowException() {
			// Given
			User user = new User(validUsername, "invalid-email-format", validPassword);
			// When Then
			ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> userRepository.saveAndFlush(user));
			String message = exception.getConstraintViolations().iterator().next().getMessage();
			assertEquals(Messages.EMAIL_INVALID_ERROR, message, "Wrong error message.");
		}
	}

	@Nested
	@DisplayName("Password constrains")
	class PasswordConstrainsTests {

		@DisplayName("❌ Negative Case: Password cannot be blank")
		@ParameterizedTest(name = "Invalid password case: \"{0}\"")
		@NullSource
		@ValueSource(strings = {"        "})
		void givenNullPassword_whenSave_thenThrowException(String invalidPassword) {
			// Given
			User user = new User(validUsername, validEmail, invalidPassword);
			// When Then
			ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> userRepository.saveAndFlush(user));
			String message = exception.getConstraintViolations().iterator().next().getMessage();
			assertEquals(Messages.PASSWORD_NOT_BLANK_ERROR, message, "Wrong error message.");
		}

		@Test
		@DisplayName("❌ Negative Case: Password too short")
		void givenTooShortPassword_whenSave_thenThrowException() {
			// Given
			String tooShortPassword = "a".repeat(UserConstants.PASSWORD_MIN_LENGTH - 1);
			User user = new User(validUsername, validEmail, tooShortPassword);
			// When Then
			ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> userRepository.saveAndFlush(user));
			String message = exception.getConstraintViolations().iterator().next().getMessage();
			assertEquals(Messages.PASSWORD_LENGTH_ERROR, message, "Wrong error message for short password.");
		}

		@Test
		@DisplayName("❌ Negative Case: Password too long")
		void givenTooLongPassword_whenSave_thenThrowException() {
			// Given
			String tooLongPassword = "a".repeat(UserConstants.PASSWORD_MAX_LENGTH + 1);
			User user = new User(validUsername, validEmail, tooLongPassword);
			// When Then
			ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> userRepository.saveAndFlush(user));
			String message = exception.getConstraintViolations().iterator().next().getMessage();
			assertEquals(Messages.PASSWORD_LENGTH_ERROR, message, "Wrong error message for short password.");
		}

		@Test
		@DisplayName("✅ Happy Case: Password at minimum length is valid")
		void givenPasswordAtMinLength_whenSave_thenNotThrowException() {
			// Given
			String validPasswordAtMin = "a".repeat(UserConstants.PASSWORD_MIN_LENGTH);
			User user = new User(validUsername, validEmail, validPasswordAtMin);
			// When Then
			assertDoesNotThrow(() -> userRepository.saveAndFlush(user));
		}

		@Test
		@DisplayName("✅ Happy Case: User registered with very long password")
		void givenLongPassword_WhenSendingPostRequest_ThenUserIsCreated() {
			// Given
			String veryLongPassword = "A1a".repeat(50); // 150 chars
			User user = new User(validUsername, validEmail, veryLongPassword);
			// When Then
			assertDoesNotThrow(() -> userRepository.saveAndFlush(user));
		}
	}

	@Nested
	@DisplayName("Birthdate constrains")
	class BirthdateConstrainsTests {
		private User user;

		@BeforeEach
		void initEach() {
			user = new User(validUsername, validEmail, validPassword);
		}

		@Test
		@DisplayName("✅ Happy Case: User with valid birthday saved successfully")
		void givenValidUserWithBirthday_whenSave_thenSuccess() {
			// Given
			user.setBirthDate(LocalDate.of(1990, 1, 1));
			// When Then
			assertDoesNotThrow(() -> {
				userRepository.saveAndFlush(user);
			});
		}

		@Test
		@DisplayName("❌ Negative Case: Birthdate must be in the past")
		void givenFutureBirthDate_whenSave_thenThrowException() {
			// Given
			user.setBirthDate(LocalDate.now().plusDays(1));
			// When Then
			ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> userRepository.saveAndFlush(user));
			String message = exception.getConstraintViolations().iterator().next().getMessage();
			assertEquals(Messages.DATE_SHOULD_BE_PAST, message, "Wrong error message.");
		}
	}
}