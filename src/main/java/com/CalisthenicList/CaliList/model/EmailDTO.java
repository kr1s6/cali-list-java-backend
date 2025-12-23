package com.CalisthenicList.CaliList.model;

import com.CalisthenicList.CaliList.constants.Messages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class EmailDTO {

	@Email(message = Messages.EMAIL_INVALID_ERROR)
	@NotBlank(message = Messages.EMAIL_INVALID_ERROR)
	private String email;
}
