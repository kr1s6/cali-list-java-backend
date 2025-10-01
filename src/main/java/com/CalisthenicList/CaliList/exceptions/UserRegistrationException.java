package com.CalisthenicList.CaliList.exceptions;

import lombok.Getter;

import java.util.Map;

@Getter
public class UserRegistrationException extends RuntimeException {
	private final Map<String, String> errors;

	public UserRegistrationException(Map<String, String> errors) {
		super("User registration failed");
		this.errors = errors;
	}
}
