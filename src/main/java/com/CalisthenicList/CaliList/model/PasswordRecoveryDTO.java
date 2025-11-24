package com.CalisthenicList.CaliList.model;

import com.CalisthenicList.CaliList.constants.Messages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import static com.CalisthenicList.CaliList.constants.UserConstants.PASSWORD_MAX_LENGTH;
import static com.CalisthenicList.CaliList.constants.UserConstants.PASSWORD_MIN_LENGTH;

@Setter
@Getter
public class PasswordRecoveryDTO {
	@Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH, message = Messages.PASSWORD_LENGTH_ERROR)
	@NotBlank(message = Messages.PASSWORD_NOT_BLANK_ERROR)
	private String password;

	@NotBlank(message = Messages.PASSWORD_NOT_BLANK_ERROR)
	private String confirmPassword;
}
