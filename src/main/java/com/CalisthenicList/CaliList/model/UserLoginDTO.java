package com.CalisthenicList.CaliList.model;

import com.CalisthenicList.CaliList.constants.Messages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.CalisthenicList.CaliList.constants.UserConstants.PASSWORD_MAX_LENGTH;

@Getter
@AllArgsConstructor
public class UserLoginDTO {

	@Email(message = Messages.EMAIL_INVALID_ERROR)
	@NotBlank(message = Messages.EMAIL_INVALID_ERROR)
	private String email;

	@Size(max = PASSWORD_MAX_LENGTH, message = Messages.PASSWORD_LENGTH_ERROR)
	@NotBlank(message = Messages.PASSWORD_NOT_BLANK_ERROR)
	private String password;
}
