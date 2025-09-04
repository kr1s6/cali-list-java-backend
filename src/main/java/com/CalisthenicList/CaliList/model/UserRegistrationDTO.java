package com.CalisthenicList.CaliList.model;

import com.CalisthenicList.CaliList.constants.Messages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.CalisthenicList.CaliList.constants.UserConstants.PASSWORD_MIN_LENGTH;
import static com.CalisthenicList.CaliList.constants.UserConstants.USERNAME_MAX_LENGTH;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDTO {

	@Size(min = 1, max = USERNAME_MAX_LENGTH, message = Messages.USERNAME_LENGTH_ERROR)
	@NotBlank(message = Messages.USERNAME_NOT_BLANK_ERROR)
	private String username;

	@Email(message = Messages.EMAIL_INVALID_ERROR)
	@NotBlank(message = Messages.EMAIL_INVALID_ERROR)
	private String email;

	@Size(min = PASSWORD_MIN_LENGTH, message = Messages.PASSWORD_LENGTH_ERROR)
	@NotBlank(message = Messages.PASSWORD_NOT_BLANK_ERROR)
	private String password;

	@Size(min = PASSWORD_MIN_LENGTH, message = Messages.PASSWORD_LENGTH_ERROR)
	@NotBlank(message = Messages.PASSWORD_NOT_BLANK_ERROR)
	private String repeatedPassword;
}
