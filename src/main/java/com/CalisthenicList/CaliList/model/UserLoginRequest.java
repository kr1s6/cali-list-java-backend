package com.CalisthenicList.CaliList.model;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UserLoginRequest {

    @NotEmpty(message = "The email address must not be empty.")
    private String email;

    @NotEmpty(message = "The password must not be empty.")
    private String password;

}
