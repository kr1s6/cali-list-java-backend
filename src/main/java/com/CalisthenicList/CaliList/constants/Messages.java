package com.CalisthenicList.CaliList.constants;

public class Messages {
	// Username
	public static final String USERNAME_LENGTH_ERROR = "The username must be between 1 and 20 characters long.";
	public static final String USERNAME_NOT_BLANK_ERROR = "The username must not be blank.";
	public static final String USERNAME_ALREADY_EXISTS_ERROR = "User with this username already exists";

	// Email
	public static final String EMAIL_INVALID_ERROR = "Invalid email address.";
	public static final String EMAIL_NOT_BLANK_ERROR = "The email address must not be blank.";
	public static final String EMAIL_ALREADY_EXISTS_ERROR = "User with this email already exists";

	// Password
	public static final String PASSWORD_LENGTH_ERROR = "The password must be at least 8 characters long.";
	public static final String PASSWORD_NOT_BLANK_ERROR = "The password must not be blank.";
	public static final String PASSWORD_INVALID_LOGIN_ERROR = "Invalid email or password";

	// Birthdate
	public static final String BIRTHDATE_PAST_ERROR = "The date of birth must be in the past.";

	// System / General
	public static final String SERVICE_ERROR = "Service error. Contact support.";
	public static final String USER_REGISTERED_SUCCESS = "User registered successfully.";
	public static final String LOGIN_SUCCESS = "Login successful";
}
