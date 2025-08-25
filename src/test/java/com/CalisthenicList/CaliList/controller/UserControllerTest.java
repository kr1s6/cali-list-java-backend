package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.enums.Roles;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private Argon2PasswordEncoder passwordEncoder;
    @LocalServerPort
    private int port;
    private String postRegisterUrl;
    private String deleteUserUrl;
    private User user;
    private HttpHeaders headers;
    private String validUsername;
    private String validEmail;
    private String validPassword;

    @BeforeEach
    void setUp() {
        user = new User();
        validUsername = "CalisthenicsAthlete";
        validEmail = "siemanoKolano@intera.pl";
        validPassword = "SiemaKolano123";
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        postRegisterUrl = "http://localhost:" + port + "/api/user/register";
        deleteUserUrl = "http://localhost:" + port + "/api/user/delete/";
    }

    @Test
    void givenValidValues_WhenSendingPostRequest_ThenSuccessfullyCreatedUserInDB() {
//        Given
        user.setUsername(validUsername);
        user.setEmail(validEmail);
        user.setPassword(validPassword);
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
//        When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
//        Then
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful(),
                "Warning! Registration failed. Code: " + response.getStatusCode());
        Assertions.assertTrue(userRepository.findByUsername(validUsername).isPresent(), "Warning! Username not found in DB");
        Assertions.assertTrue(userRepository.findByEmail(validEmail).isPresent(), "Warning! Email not found in DB");
        User createdUser = userRepository.findByEmail(validEmail).get();
        Assertions.assertNotNull(createdUser.getCreatedDate(), "Warning! Created Date is null");
        Assertions.assertNotNull(createdUser.getUpdatedDate(), "Warning! Updated Date is null");
        Assertions.assertEquals(Roles.ROLE_USER, createdUser.getRole(), "Warning! User has wrong role.");
        Assertions.assertTrue(passwordEncoder.matches(validPassword, createdUser.getPassword()),
                "Warning! Password isn't properly encrypted");
        // Cleanup
        testRestTemplate.delete(deleteUserUrl + validEmail);
    }

    @Test
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
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful(),
                "Warning! Registration failed. Code: " + response.getStatusCode());
        Assertions.assertTrue(userRepository.findByEmail(validEmail).isPresent(), "Warning! Email not found in DB");
        User createdUser = userRepository.findByEmail(validEmail).get();
        Assertions.assertTrue(passwordEncoder.matches(veryLongPassword, createdUser.getPassword()),
                "Warning! Password isn't properly encrypted");
        // Cleanup
        testRestTemplate.delete(deleteUserUrl + validEmail);
    }

    @Test
    void givenNullPassword_WhenSendingPostRequest_ThenUserIsNotCreated() {
//        Given
        user.setUsername(validUsername);
        user.setEmail(validEmail);
        user.setPassword(null);
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
//        When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
//        Then
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed with null password. Code: " + response.getStatusCode());
        Assertions.assertFalse(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email found in DB");
        Assertions.assertTrue(response.getBody().contains("The password must not be empty."), "Warning! Wrong error message");
    }

    @Test
    void givenEmptyPassword_WhenSendingPostRequest_ThenUserIsNotCreated() {
//        Given
        user.setUsername(validUsername);
        user.setEmail(validEmail);
        user.setPassword("");
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
//        When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
//        Then
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed with empty password. Code: " + response.getStatusCode());
        Assertions.assertFalse(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email found in DB");
        Assertions.assertTrue(response.getBody().contains("The password must not be empty."), "Warning! Wrong error message");
    }

    @Test
    void givenBlankPassword_WhenSendingPostRequest_ThenUserIsNotCreated() {
//        Given
        String invalidPassword = "          ";
        user.setUsername(validUsername);
        user.setEmail(validEmail);
        user.setPassword(invalidPassword);
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
//        When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
//        Then
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed with blank password. Code: " + response.getStatusCode());
        Assertions.assertFalse(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email found in DB");
        Assertions.assertTrue(response.getBody().contains("The password must not be empty."), "Warning! Wrong error message");
    }

    @Test
    void givenTooShortPassword_WhenSendingPostRequest_ThenUserIsNotCreated() {
//        Given
        user.setUsername(validUsername);
        user.setEmail(validEmail);
        user.setPassword("Invalid");
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
//        When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
//        Then
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed with too short password. Code: " + response.getStatusCode());
        Assertions.assertFalse(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email found in DB");
        Assertions.assertTrue(response.getBody().contains("The password must be at least 8 characters long."), "Warning! Wrong error message");
    }

    @Test
    void givenEmptyEmail_WhenSendingPostRequest_ThenUserIsNotCreated() {
        // Given
        user.setUsername(validUsername);
        user.setEmail("");
        user.setPassword(validPassword);
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
        // Then
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed with empty email. Code: " + response.getStatusCode());
        Assertions.assertFalse(userRepository.findByUsername(validUsername).isPresent(), "Warning! User should not exist in DB");
        Assertions.assertTrue(response.getBody().contains("The email address must not be empty."), "Warning! Wrong error message");
    }

    @Test
    void givenInvalidEmailFormat_WhenSendingPostRequest_ThenUserIsNotCreated() {
        // Given
        user.setUsername(validUsername);
        user.setEmail("invalid-email-format"); // no @
        user.setPassword(validPassword);
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
        // Then
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed with invalid email. Code: " + response.getStatusCode());
        Assertions.assertFalse(userRepository.findByUsername(validUsername).isPresent(), "Warning! User should not exist in DB");
        Assertions.assertTrue(response.getBody().contains("Invalid email address."), "Warning! Wrong error message");
    }

    @Test
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
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed with duplicate email. Code: " + response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("User with this email already exists"), "Warning! Wrong error message");
        // Cleanup
        userRepository.delete(validUser);
    }

    @Test
    void givenEmptyUsername_WhenSendingPostRequest_ThenUserIsNotCreated() {
        // Given
        user.setUsername("");
        user.setEmail(validEmail);
        user.setPassword(validPassword);
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
        // Then
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed with empty username. Code: " + response.getStatusCode());
        Assertions.assertFalse(userRepository.findByEmail(validEmail).isPresent(), "Warning! User should not exist in DB");
        Assertions.assertTrue(response.getBody().contains("The username must not be blank."), "Warning! Wrong error message");
    }

    @Test
    void givenBlankUsername_WhenSendingPostRequest_ThenUserIsNotCreated() {
        // Given
        user.setUsername("    ");
        user.setEmail(validEmail);
        user.setPassword(validPassword);
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
        // Then
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed with blank username. Code: " + response.getStatusCode());
        Assertions.assertFalse(userRepository.findByEmail(validEmail).isPresent(), "Warning! User should not exist in DB");
        Assertions.assertTrue(response.getBody().contains("The username must not be blank."), "Warning! Wrong error message");
    }

    @Test
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
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed with too long username. Code: " + response.getStatusCode());
        Assertions.assertFalse(userRepository.findByEmail(validEmail).isPresent(), "Warning! User should not exist in DB");
        Assertions.assertTrue(response.getBody().contains("The username must be between 1 and 30 characters long."), "Warning! Wrong error message");
    }

    @Test
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
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed with duplicate username. Code: " + response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("User with this username already exists"), "Warning! Wrong error message");
        // Cleanup
        userRepository.delete(validUser);
    }
}