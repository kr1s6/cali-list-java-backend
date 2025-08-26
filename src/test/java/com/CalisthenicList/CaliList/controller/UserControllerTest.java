package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.enums.Roles;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {
    private static HttpHeaders headers;
    @LocalServerPort
    private int port;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private Argon2PasswordEncoder passwordEncoder;

    @BeforeAll
    static void init() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Nested
    @DisplayName("User registration tests")
    class UserControllerTestRegister {
        private static User user;
        private static String validUsername;
        private static String validEmail;
        private static String validPassword;
        private String postRegisterUrl;
        private String deleteUserUrl;

        @BeforeAll
        static void init() {
            user = new User();
            validUsername = "CalisthenicsAthlete";
            validEmail = "siemanoKolano@intera.pl";
            validPassword = "SiemaKolano123";
        }

        @BeforeEach
        void initAll() {
            postRegisterUrl = "http://localhost:" + port + "/api/user/register";
            deleteUserUrl = "http://localhost:" + port + "/api/user/delete/";
        }

        @Test
        @DisplayName("✅ Happy Case: User registered with valid credentials")
        void givenValidValues_WhenSendingPostRequest_ThenSuccessfullyCreatedUserInDB() {
//        Given
            user.setUsername(validUsername);
            user.setEmail(validEmail);
            user.setPassword(validPassword);
            HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
//        When
            ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
//        Then
            assertTrue(response.getStatusCode().is2xxSuccessful(),
                    "Warning! Registration failed. Code: " + response.getStatusCode());
            assertTrue(userRepository.findByUsername(validUsername).isPresent(), "Warning! Username not found in DB");
            assertTrue(userRepository.findByEmail(validEmail).isPresent(), "Warning! Email not found in DB");
            User createdUser = userRepository.findByEmail(validEmail).get();
            assertNotNull(createdUser.getCreatedDate(), "Warning! Created Date is null");
            assertNotNull(createdUser.getUpdatedDate(), "Warning! Updated Date is null");
            assertEquals(Roles.ROLE_USER, createdUser.getRole(), "Warning! User has wrong role.");
            assertTrue(passwordEncoder.matches(validPassword, createdUser.getPassword()),
                    "Warning! Password isn't properly encrypted");
            // Cleanup
            testRestTemplate.delete(deleteUserUrl + validEmail);
        }

        @Test
        @DisplayName("✅ Happy Case: User registered with long password")
        void givenLongPassword_WhenSendingPostRequest_ThenUserIsCreated() {
            // Given
            String veryLongPassword = "A1a".repeat(50); // 150 chars
            user.setUsername(validUsername);
            user.setEmail(validEmail);
            user.setPassword(veryLongPassword);
            HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
            // When
            ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
            // Then
            assertTrue(response.getStatusCode().is2xxSuccessful(),
                    "Warning! Registration failed. Code: " + response.getStatusCode());
            assertTrue(userRepository.findByEmail(validEmail).isPresent(), "Warning! Email not found in DB");
            User createdUser = userRepository.findByEmail(validEmail).get();
            assertTrue(passwordEncoder.matches(veryLongPassword, createdUser.getPassword()),
                    "Warning! Password isn't properly encrypted");
            // Cleanup
            testRestTemplate.delete(deleteUserUrl + validEmail);
        }

        @Test
        @DisplayName("❌ Negative Case: Too short password")
        void givenTooShortPassword_WhenSendingPostRequest_ThenUserIsNotCreated() {
//        Given
            user.setUsername(validUsername);
            user.setEmail(validEmail);
            user.setPassword("Invalid");
            HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
//        When
            ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
//        Then
            assertTrue(response.getStatusCode().is4xxClientError(),
                    "Warning! Registration passed with too short password. Code: " + response.getStatusCode());
            assertFalse(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email found in DB");
            assertTrue(response.getBody().contains("The password must be at least 8 characters long."), "Warning! Wrong error message");
        }

        @Test
        @DisplayName("❌ Negative Case: Too long username")
        void givenTooLongUsername_WhenSendingPostRequest_ThenUserIsNotCreated() {
            // Given
            String longUsername = "A".repeat(21); // 21 chars
            user.setUsername(longUsername);
            user.setEmail(validEmail);
            user.setPassword(validPassword);
            HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
            // When
            ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
            // Then
            assertTrue(response.getStatusCode().is4xxClientError(),
                    "Warning! Registration passed with too long username. Code: " + response.getStatusCode());
            assertFalse(userRepository.findByEmail(validEmail).isPresent(), "Warning! User should not exist in DB");
            assertTrue(response.getBody().contains("The username must be between 1 and 30 characters long."), "Warning! Wrong error message");
        }

        @DisplayName("❌ Negative Case: Blank password")
        @ParameterizedTest(name = "Invalid password case: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"        "})
        void givenBlankPassword_WhenSendingPostRequest_ThenUserIsNotCreated(String invalidPassword) {
//        Given
            user.setUsername(validUsername);
            user.setEmail(validEmail);
            user.setPassword(invalidPassword);
            HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
//        When
            ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
//        Then
            assertTrue(response.getStatusCode().is4xxClientError(),
                    "Warning! Registration passed with invalid password. Code: " + response.getStatusCode());
            assertFalse(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email found in DB");
            assertTrue(response.getBody().contains("The password must not be blank."), "Warning! Wrong error message");
        }

        @DisplayName("❌ Negative Case: Blank email")
        @ParameterizedTest(name = "Invalid email case: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"        "})
        void givenBlankEmail_WhenSendingPostRequest_ThenUserIsNotCreated(String invalidEmail) {
            // Given
            user.setUsername(validUsername);
            user.setEmail(invalidEmail);
            user.setPassword(validPassword);
            HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
            // When
            ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
            // Then
            assertTrue(response.getStatusCode().is4xxClientError(),
                    "Warning! Registration passed with empty email. Code: " + response.getStatusCode());
            assertFalse(userRepository.findByUsername(validUsername).isPresent(), "Warning! User should not exist in DB");
            assertTrue(response.getBody().contains("The email address must not be blank."), "Warning! Wrong error message");
        }

        @DisplayName("❌ Negative Case: Blank username")
        @ParameterizedTest(name = "Invalid username case: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"        "})
        void givenBlankUsername_WhenSendingPostRequest_ThenUserIsNotCreated(String invalidUsername) {
            // Given
            user.setUsername(invalidUsername);
            user.setEmail(validEmail);
            user.setPassword(validPassword);
            HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
            // When
            ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
            // Then
            assertTrue(response.getStatusCode().is4xxClientError(),
                    "Warning! Registration passed with invalid username. Code: " + response.getStatusCode());
            assertFalse(userRepository.findByEmail(validEmail).isPresent(), "Warning! User should not exist in DB");
            assertTrue(response.getBody().contains("The username must not be blank."), "Warning! Wrong error message");
        }

        @Test
        @DisplayName("❌ Negative Case: Invalid format email")
        void givenInvalidEmailFormat_WhenSendingPostRequest_ThenUserIsNotCreated() {
            // Given
            user.setUsername(validUsername);
            user.setEmail("invalid-email-format"); // no @
            user.setPassword(validPassword);
            HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
            // When
            ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
            // Then
            assertTrue(response.getStatusCode().is4xxClientError(),
                    "Warning! Registration passed with invalid email. Code: " + response.getStatusCode());
            assertFalse(userRepository.findByUsername(validUsername).isPresent(), "Warning! User should not exist in DB");
            assertTrue(response.getBody().contains("Invalid email address."), "Warning! Wrong error message");
        }

        @Test
        @DisplayName("❌ Negative Case: Already registered email")
        void givenExistingEmail_WhenSendingPostRequest_ThenUserIsNotCreated() {
            // Given
            User validUser = new User();
            validUser.setUsername("DifferentUsername");
            validUser.setEmail(validEmail);
            validUser.setPassword(validPassword);
            userRepository.save(validUser);
            user.setUsername(validUsername);
            user.setEmail(validEmail);
            user.setPassword(validPassword);
            HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
            // When
            ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
            // Then
            assertTrue(response.getStatusCode().is4xxClientError(),
                    "Warning! Registration passed with duplicate email. Code: " + response.getStatusCode());
            assertTrue(response.getBody().contains("User with this email already exists"), "Warning! Wrong error message");
            // Cleanup
            userRepository.delete(validUser);
        }

        @Test
        @DisplayName("❌ Negative Case: Already existing username")
        void givenExistingUsername_WhenSendingPostRequest_ThenUserIsNotCreated() {
            // Given
            User validUser = new User();
            validUser.setUsername(validUsername);
            validUser.setEmail("different@user.com");
            validUser.setPassword(validPassword);
            userRepository.save(validUser);
            user.setUsername(validUsername);
            user.setEmail(validEmail);
            user.setPassword(validPassword);
            HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
            // When
            ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
            // Then
            assertTrue(response.getStatusCode().is4xxClientError(),
                    "Warning! Registration passed with duplicate username. Code: " + response.getStatusCode());
            assertTrue(response.getBody().contains("User with this username already exists"), "Warning! Wrong error message");
            // Cleanup
            userRepository.delete(validUser);
        }
    }

    @Nested
    @DisplayName("User login tests")
    class UserControllerTestLogin {
    }

    @Nested
    @DisplayName("Delete user tests")
    class UserControllerTestDeleteUser {
    }
}