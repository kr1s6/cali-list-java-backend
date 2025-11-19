package com.CalisthenicList.CaliList.model;

import com.CalisthenicList.CaliList.constants.Messages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDeleteByIdDTO {
	@NotNull(message = "ID must not be null.")
	private UUID userId;
	@NotBlank(message = Messages.PASSWORD_NOT_BLANK_ERROR)
	private String password;
}
