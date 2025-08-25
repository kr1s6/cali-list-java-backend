package com.CalisthenicList.CaliList.controller;

import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.model.UserLoginRequest;
import com.CalisthenicList.CaliList.service.UserControllerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserControllerService userControllerService;

    public UserController(UserControllerService userControllerService) {
        this.userControllerService = userControllerService;
    }

    @PostMapping("/register")
    //INFO registration request require Unique username, Unique email, password
    public ResponseEntity<List<String>> register(@Valid @RequestBody User user) {
        return userControllerService.registrationService(user);
//        TODO
//         - Include a password strength meter (zxcvbn-ts library) (Frontend)
//         - Include confirm password tile only on frontend side for user-friendly authentication (Frontend)
//         - Block common and previously breached passwords by pwned password
//         - check if this endpoint need to return user object or you get it differently
//         - Implement Secure Password Recovery Mechanism
    }

    @PostMapping("/login")
    public ResponseEntity<List<String>> login(@Valid @RequestBody UserLoginRequest userLoginRequest) {
        return userControllerService.loginService(userLoginRequest);
//        TODO
//         - User can use email or username as a [username]
//         - "three strikes and you are out" policy is the pain for legitimate user
    }


    @DeleteMapping("/delete/{email}")
    public ResponseEntity<String> deleteUser(@PathVariable String email) {
        return userControllerService.deleteUserService(email);
//      TODO
//       - need to be secured for admin, tests and for user to delete himself
    }
}






