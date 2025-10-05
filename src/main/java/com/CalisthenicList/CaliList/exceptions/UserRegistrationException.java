package com.CalisthenicList.CaliList.exceptions;

import com.CalisthenicList.CaliList.constants.Messages;
import lombok.Getter;

import java.util.Map;

@Getter
public class UserRegistrationException extends RuntimeException {
	private final Map<String, String> errors;

	public UserRegistrationException(Map<String, String> errors) {
		super(Messages.USER_REGISTERED_FAILED);
		this.errors = errors;
	}
}
