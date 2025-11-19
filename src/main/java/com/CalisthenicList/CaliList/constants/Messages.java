package com.CalisthenicList.CaliList.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class Messages {
	// Username
	public static final String USERNAME_LENGTH_ERROR = "The username must be between 1 and 30 characters long.";
	public static final String USERNAME_NOT_BLANK_ERROR = "The username must not be empty.";
	public static final String USERNAME_ALREADY_EXISTS_ERROR = "Username already exists.";

	// Email
	public static final String EMAIL_INVALID_ERROR = "Invalid email address.";
	public static final String EMAIL_ALREADY_EXISTS_ERROR = "Email already exists.";
	public static final String EMAIL_ALREADY_VERIFIED = "Email is already verified.";
	public static final String EMAIL_VERIFICATION_SUCCESS = "Email verification successful.";
	public static final String TOKEN_INVALID = "Token Invalid. Generate new token in your profile settings.";

	// Password
	public static final String PASSWORD_LENGTH_ERROR = "The password must be at least 8 characters long.";
	public static final String PASSWORD_NOT_BLANK_ERROR = "The password must not be empty.";
	public static final String INVALID_LOGIN_ERROR = "Invalid email or password.";
	public static final String INVALID_CONFIRM_PASSWORD_ERROR = "Wrong confirm password.";

	// Birthdate
	public static final String DATE_SHOULD_BE_PAST = "Put date in the past.";

	// System / General
	public static final String SERVICE_ERROR = "Service error. Contact support.";
	public static final String USER_NOT_FOUND = "User not found.";
	public static final String UNAUTHORIZED = "Error: Unauthorized";
	public static final String USER_REGISTERED_SUCCESS = "User registered successfully.";
	public static final String USER_REGISTERED_FAILED = "User registration failed.";
	public static final String LOGIN_SUCCESS = "Login successful.";
	public static final String USER_DELETED = "User deleted successfully";
	public static final String REFRESH_TOKEN_SUCCESS = "Token refreshed successfully";
	public static final String VALIDATION_FAILED = "Validation failed.";

}
