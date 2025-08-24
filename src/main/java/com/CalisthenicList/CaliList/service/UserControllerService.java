package com.CalisthenicList.CaliList.service;

import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.model.UserLoginRequest;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class UserControllerService {
    private static final Logger logger = Logger.getLogger(UserControllerService.class.getName());
    private final UserRepository userRepository;
    private final Argon2PasswordEncoder encoder;

    public UserControllerService(UserRepository userRepository, Argon2PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    public ResponseEntity<List<String>> registrationService(User user) {
        String username = user.getUsername();
        String email = user.getEmail();
        String notHashedPassword = user.getPassword();
        String confirmPassword = user.getConfirmPassword();
        boolean emailAlreadyExists = userRepository.findByEmail(email).isPresent();
        boolean usernameAlreadyExists = userRepository.findByUsername(username).isPresent();
        boolean invalidConfirmationPassword = !notHashedPassword.equals(confirmPassword);
        List<String> responseBody = new ArrayList<>();

        if (emailAlreadyExists) {
            responseBody.add("User with this email already exists");
            logger.warning("Attempted registration with existing email: " + email);
        }
        if (usernameAlreadyExists) {
            responseBody.add("User with this username already exists");
            logger.warning("Attempted registration with existing username: " + username);
        }
        if (invalidConfirmationPassword) {
            responseBody.add("Passwords do not match");
            logger.warning("Attempted registration with invalid confirmation password: " + confirmPassword);
        }
        if (emailAlreadyExists || usernameAlreadyExists || invalidConfirmationPassword) {
            return new ResponseEntity<>(responseBody, HttpStatus.CONFLICT);
        }

        user.setPassword(encoder.encode(notHashedPassword));
        if (user.getPassword().equals(notHashedPassword)) {
            responseBody.add("Service error. Contact support.");
            logger.warning("Password encoding failed.");
            return new ResponseEntity<>(responseBody, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        userRepository.save(user);
        responseBody.add("User registered successfully.");
        return new ResponseEntity<>(responseBody, HttpStatus.CREATED);
    }

    public ResponseEntity<List<String>> loginService(UserLoginRequest userLoginRequest) {
        List<String> responseBody = new ArrayList<>();
        Optional<User> userOptional = userRepository.findByEmail(userLoginRequest.getEmail());
        if (userOptional.isEmpty()) {
            logger.warning("Login attempt with non-existing email: " + userLoginRequest.getEmail());
            responseBody.add("Invalid email or password");
            return new ResponseEntity<>(responseBody, HttpStatus.UNAUTHORIZED);
        }

        User user = userOptional.get();
        boolean matches = encoder.matches(userLoginRequest.getPassword(), user.getPassword());
        if (!matches) {
            logger.warning("Invalid password attempt for email: " + userLoginRequest.getEmail());
            responseBody.add("Invalid email or password");
            return new ResponseEntity<>(responseBody, HttpStatus.UNAUTHORIZED);
        }
        logger.info("User logged in successfully: " + userLoginRequest.getEmail());
        responseBody.add("Login successful");
        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    public ResponseEntity<String> deleteUserService(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            logger.warning("Attempt to delete non-existing user: " + email);
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        User userToDelete = userOptional.get();
        userRepository.delete(userToDelete);
        return new ResponseEntity<>("User " + email + " deleted successfully", HttpStatus.OK);
    }
}










