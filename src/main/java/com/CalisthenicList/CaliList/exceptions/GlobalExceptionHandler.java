package com.CalisthenicList.CaliList.exceptions;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.model.ApiResponse;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
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

	@ExceptionHandler(UserRegistrationException.class)
	public ResponseEntity<ApiResponse<Object>> handleUserRegistration(UserRegistrationException ex) {
		logger.log(Level.WARNING, ex.getMessage(), ex);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(
				ApiResponse.builder()
						.success(false)
						.message(ex.getMessage())
						.data(ex.getErrors())
						.build()
		);
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
		logger.log(Level.WARNING, ex.getMessage(), ex);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
				ApiResponse.builder()
						.success(false)
						.message(ex.getMessage())
						.data(ex.getMessage())
						.build()
		);
	}

	/**
	 * Handle errors during login
	 * @param ex
	 * @return {@link ApiResponse}
	 */
	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<ApiResponse<Object>> handleUserNotFound(UsernameNotFoundException ex) {
		logger.log(Level.WARNING, ex.getMessage(), ex);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
				ApiResponse.builder()
						.success(false)
						.message(ex.getMessage())
						.data(ex.getMessage())
						.build()
		);
	}

	/**
	 * Handle errors thrown by @Valid annotation
	 * @param ex
	 * @return {@link ApiResponse} with a list of validation errors
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
		logger.log(Level.WARNING, ex.getMessage(), ex);
		List<String> errors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(FieldError::getDefaultMessage)
				.collect(Collectors.toList());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
				ApiResponse.builder()
						.success(false)
						.message(Messages.VALIDATION_FAILED)
						.data(errors)
						.build()
		);
	}

	/**
	 * Handle JWT errors
	 * @param ex Error data
	 * @return Error message
	 */
	@ExceptionHandler(JwtException.class)
	public ResponseEntity<ApiResponse<Object>> handleJwtExceptions(JwtException ex) {
		logger.log(Level.WARNING, ex.getMessage(), ex);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
				ApiResponse.builder()
						.success(false)
						.message(ex.getMessage())
						.build()
		);
	}

	/**
	 * Handle unexpected errors and INTERNAL_SERVER_ERRORS
	 * @param ex Error data
	 * @return {@link ApiResponse}
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Object>> handleUnpredictedExceptions(Exception ex) {
		logger.log(Level.SEVERE, ex.getMessage(), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
				ApiResponse.builder()
						.success(false)
						.message("Internal server error")
						.data(ex.getMessage())
						.build()
		);
	}

	private Map<String, List<String>> getErrorsMap(List<String> errors) {
		Map<String, List<String>> errorResponse = new HashMap<>();
		errorResponse.put("errors", errors);
		return errorResponse;
	}
}
