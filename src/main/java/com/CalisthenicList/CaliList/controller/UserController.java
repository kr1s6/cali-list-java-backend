package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.model.UserLoginRequest;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.logging.Logger;


@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private static final Logger logger = Logger.getLogger(UserController.class.getName());

    public UserController(UserRepository userRepository, BCryptPasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            logger.warning("Attempted registration with existing email: " + user.getEmail());
            return new ResponseEntity<>("User with this email already exists", HttpStatus.CONFLICT);
        }
        user.setPassword(encoder.encode(user.getPassword()));
        userRepository.save(user);
        return new ResponseEntity<>("User registered successfully.", HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody UserLoginRequest userLoginRequest) {
        Optional<User> userOptional = userRepository.findByEmail(userLoginRequest.getEmail());
        if (userOptional.isEmpty()) {
            logger.warning("Login attempt with non-existing email: " + userLoginRequest.getEmail());
            return new ResponseEntity<>("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        User user = userOptional.get();
        boolean matches = encoder.matches(userLoginRequest.getPassword(), user.getPassword());
        if (!matches) {
            logger.warning("Invalid password attempt for email: " + userLoginRequest.getEmail());
            return new ResponseEntity<>("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }
        logger.info("User logged in successfully: " + userLoginRequest.getEmail());
        return new ResponseEntity<>("Login successful", HttpStatus.OK);
    }
}
