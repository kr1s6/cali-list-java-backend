package com.CalisthenicList.CaliList.model.DTO;

import com.CalisthenicList.CaliList.constants.Messages;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserBirthdateDTO {
	@Past(message = Messages.DATE_SHOULD_BE_PAST)
	@NotNull(message = Messages.INVALID_INPUT)
	private LocalDate birthdate;
}
