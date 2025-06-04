package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<? extends Object> addUser(@Valid @RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return new ResponseEntity<>("User with this email already exists", HttpStatus.CONFLICT);
        }
        user.setPassword(encoder.encode(user.getPassword()));
        User insertedUserDto = userRepository.save(user);
        return new ResponseEntity<>(insertedUserDto, HttpStatus.CREATED);

    }
}
