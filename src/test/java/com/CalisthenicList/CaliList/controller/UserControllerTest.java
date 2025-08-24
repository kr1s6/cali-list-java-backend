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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @LocalServerPort
    private int port;
    private String postRegisterUrl;
    private String deleteUserUrl;
    private User user;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        user = new User();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        postRegisterUrl = "http://localhost:" + port + "/api/user/register";
        deleteUserUrl = "http://localhost:" + port + "/api/user/delete/";
    }

    @Test
    void givenValidValues_WhenSendingPostRequest_ThenSuccessfullyCreatedUserInDB() {
//        Given
        String password = "SiemaKolano123";
        String email = "siemanoKolano@intera.pl";
        String username = "CalisthenicsAthlete";
        user.setEmail(email);
        user.setPassword(password);
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
//        When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
//        Then
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful(),
                "Warning! Registration failed. Code: " + response.getStatusCode());
        Assertions.assertTrue(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email not found in DB");
        User createdUser = userRepository.findByEmail(user.getEmail()).get();
        Assertions.assertNotNull(createdUser.getCreatedDate(), "Warning! Created Date is null");
        Assertions.assertNotNull(createdUser.getUpdatedDate(), "Warning! Updated Date is null");
        Assertions.assertEquals(Roles.ROLE_USER, createdUser.getRole(), "Warning! User has wrong role.");
        Assertions.assertEquals(username, createdUser.getName(), "Warning! Unexpected username");
        Assertions.assertTrue(passwordEncoder.matches(password, createdUser.getPassword()),
                "Warning! Password isn't properly encrypted");

        testRestTemplate.delete(deleteUserUrl + email);
    }

    @Test
//    TODO ogarnij co zrobić gdy user wpisze długi hasło bo system tego nie ogarnia
    void givenLongPassword_WhenSendingPostRequest_ThenUserIsCreated() {
        // Given
        String veryLongPassword = "A1a".repeat(40); // 201 chars
        int ile = veryLongPassword.length();
        int bytes = veryLongPassword.getBytes().length;
        String email = "siemanoKolano@intera.pl";
        user.setEmail(email);
        user.setPassword(veryLongPassword);
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
        // Then
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful(),
                "Warning! Registration failed. Code: " + response.getStatusCode());
        Assertions.assertTrue(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email not found in DB");
        testRestTemplate.delete(deleteUserUrl + email);
    }

//    TODO The best way to store a password would be to run a heavy kdf like Scrypt on the client side to convert the password
//     into a fixed-sized hash before it's sent to the server anyway, then just hash the fixed-sized hash one more time on the server.


    @Test
    void givenNullPassword_WhenSendingPostRequest_ThenUserIsNotCreated() {
//        Given
        user.setEmail("siemanoKolano@intera.pl");
        user.setPassword(null);
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
//        When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
//        Then
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed with null password. Code: " + response.getStatusCode());
        Assertions.assertFalse(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email found in DB");
    }

    @Test
    void givenEmptyPassword_WhenSendingPostRequest_ThenUserIsNotCreated() {
//        Given
        user.setEmail("siemanoKolano@intera.pl");
        user.setPassword("");
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
//        When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
//        Then
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed with empty password. Code: " + response.getStatusCode());
        Assertions.assertFalse(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email found in DB");
    }

    @Test
    void givenBlankPassword_WhenSendingPostRequest_ThenUserIsNotCreated() {
//        Given
        user.setEmail("siemanoKolano@intera.pl");
        user.setPassword("        ");
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
//        When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
//        Then
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed with blank password. Code: " + response.getStatusCode());
        Assertions.assertFalse(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email found in DB");
    }

    @Test
    void givenTooShortPassword_WhenSendingPostRequest_ThenUserIsNotCreated() {
//        Given
        user.setEmail("siemanoKolano@intera.pl");
        user.setPassword("Wrong11");
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
//        When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
//        Then
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed with too short password. Code: " + response.getStatusCode());
        Assertions.assertFalse(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email found in DB");
    }

    @Test
    void givenPasswordWithoutUppercase_WhenSendingPostRequest_ThenUserIsNotCreated() {
        // Given
        user.setEmail("siemanoKolano@intera.pl");
        user.setPassword("password123");
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
        // Then
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed without uppercase letter. Code: " + response.getStatusCode());
        Assertions.assertFalse(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email found in DB");
    }

    @Test
    void givenPasswordWithoutLowercase_WhenSendingPostRequest_ThenUserIsNotCreated() {
        // Given
        user.setEmail("siemanoKolano@intera.pl");
        user.setPassword("PASSWORD123");
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
        // Then
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed without lowercase letter. Code: " + response.getStatusCode());
        Assertions.assertFalse(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email found in DB");
    }

    @Test
    void givenPasswordWithoutNumber_WhenSendingPostRequest_ThenUserIsNotCreated() {
        // Given
        user.setEmail("siemanoKolano@intera.pl");
        user.setPassword("Password");
        HttpEntity<User> registrationRequest = new HttpEntity<>(user, headers);
        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(postRegisterUrl, registrationRequest, String.class);
        // Then
        Assertions.assertTrue(response.getStatusCode().is4xxClientError(),
                "Warning! Registration passed without number. Code: " + response.getStatusCode());
        Assertions.assertFalse(userRepository.findByEmail(user.getEmail()).isPresent(), "Warning! Email found in DB");
    }


}