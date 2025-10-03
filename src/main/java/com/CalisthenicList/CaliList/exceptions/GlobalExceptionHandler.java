package com.CalisthenicList.CaliList.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger logger = Logger.getLogger(GlobalExceptionHandler.class.getName());

	//Handle errors during login
	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleUserNotFound(UsernameNotFoundException ex) {
		logger.log(Level.WARNING, ex.getMessage(), ex); Map<String, String> error = Map.of("error", ex.getMessage());
		return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(UserRegistrationException.class)
	public ResponseEntity<Map<String, Map<String, String>>> handleUserRegistration(UserRegistrationException ex) {
		logger.log(Level.WARNING, ex.getMessage(), ex);
		return new ResponseEntity<>(Map.of("errors", ex.getErrors()), HttpStatus.CONFLICT);
	}

	//Handle errors thrown by @Valid annotation
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, List<String>>> handleValidationErrors(MethodArgumentNotValidException ex) {
		logger.log(Level.WARNING, ex.getMessage(), ex);
		List<String> errors = ex.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage).collect(Collectors.toList());
		return new ResponseEntity<>(getErrorsMap(errors), HttpStatus.BAD_REQUEST);
	}

	//Handle unexpected errors and INTERNAL_SERVER_ERRORS
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleUnpredictedExceptions(Exception ex) {
		logger.log(Level.SEVERE, ex.getMessage(), ex);
		Map<String, String> error = Map.of("error", "An unexpected error occurred: " + ex.getMessage());
		return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private Map<String, List<String>> getErrorsMap(List<String> errors) {
		Map<String, List<String>> errorResponse = new HashMap<>(); errorResponse.put("errors", errors);
		return errorResponse;
	}
}
