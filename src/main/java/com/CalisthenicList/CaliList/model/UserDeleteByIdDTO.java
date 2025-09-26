package com.CalisthenicList.CaliList.model;

import com.CalisthenicList.CaliList.constants.Messages;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserDeleteByIdDTO {
	@NotBlank(message = "ID must not be empty.")
	private UUID userId;
	@NotBlank(message = Messages.PASSWORD_NOT_BLANK_ERROR)
	private String password;
}
