package userservice.config;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import userservice.dtos.ErrorResponse;
import userservice.exceptions.ResourceNotFoundException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global Exception Handler for the entire application.
 *
 * <p>
 * This class centralizes exception handling logic and ensures:
 * - Consistent API error responses
 * - Proper HTTP status codes
 * - Cleaner controller/service code (no try-catch clutter)
 * </p>
 *
 * <p>
 * NOTE:
 * Spring automatically routes exceptions to the most specific handler.
 * Generic handler (Exception.class) should always be LAST.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Handles validation errors triggered by @Valid / @Validated.
	 *
	 * Example:
	 * - Missing email
	 * - Blank password
	 * - Invalid request body fields
	 *
	 * Returns:
	 * - HTTP 400 (Bad Request)
	 * - Field-wise error messages
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(
			MethodArgumentNotValidException ex) {

		Map<String, String> errors = new HashMap<>();

		// Extract field-level validation errors
		ex.getBindingResult().getFieldErrors()
				.forEach(error ->
						errors.put(error.getField(), error.getDefaultMessage())
				);

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(errors);
	}

	/**
	 * Handles authentication failures due to invalid credentials.
	 *
	 * Triggered when:
	 * - Login with incorrect email/password
	 * - Incorrect old password during password change
	 *
	 * Returns:
	 * - HTTP 401 (Unauthorized)
	 */
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleBadCredentials(
			BadCredentialsException ex) {

		ErrorResponse error = ErrorResponse.builder()
				.timestamp(LocalDateTime.now())
				.status(HttpStatus.UNAUTHORIZED.value())
				.error("Unauthorized")
				.message(ex.getMessage()) // Avoid exposing sensitive details in production if needed
				.build();

		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(error);
	}

	/**
	 * Handles disabled user accounts.
	 *
	 * Returns:
	 * - HTTP 401 (Unauthorized)
	 */
	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex) {

		ErrorResponse error = ErrorResponse.builder()
				.timestamp(LocalDateTime.now())
				.status(HttpStatus.UNAUTHORIZED.value())
				.error("Unauthorized")
				.message("Account is disabled")
				.build();

		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(error);
	}

	/**
	 * Handles locked user accounts.
	 *
	 * Returns:
	 * - HTTP 401 (Unauthorized)
	 */
	@ExceptionHandler(LockedException.class)
	public ResponseEntity<ErrorResponse> handleLocked(LockedException ex) {

		ErrorResponse error = ErrorResponse.builder()
				.timestamp(LocalDateTime.now())
				.status(HttpStatus.UNAUTHORIZED.value())
				.error("Unauthorized")
				.message("Account is locked")
				.build();

		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(error);
	}

	/**
	 * Handles custom "resource not found" exceptions.
	 *
	 * Example:
	 * - User not found
	 * - Record not found in database
	 *
	 * Returns:
	 * - HTTP 404 (Not Found)
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(
			ResourceNotFoundException ex) {

		ErrorResponse error = ErrorResponse.builder()
				.timestamp(LocalDateTime.now())
				.status(HttpStatus.NOT_FOUND.value())
				.error("Not Found")
				.message(ex.getMessage())
				.build();

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(error);
	}

	/**
	 * Handles database constraint violations.
	 *
	 * Example:
	 * - Duplicate email/username
	 * - Unique constraint violations
	 *
	 * Returns:
	 * - HTTP 409 (Conflict)
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
			DataIntegrityViolationException ex) {

		String message = "Database constraint violation";

		// Extract root cause for better error message
		String cause = ex.getMostSpecificCause().getMessage();
		if (cause != null && cause.contains("Duplicate")) {
			message = "Duplicate value detected. Please use a unique value.";
		}

		ErrorResponse error = ErrorResponse.builder()
				.timestamp(LocalDateTime.now())
				.status(HttpStatus.CONFLICT.value())
				.error("Conflict")
				.message(message)
				.build();

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(error);
	}

	/**
	 * Fallback handler for all unhandled exceptions.
	 *
	 * IMPORTANT:
	 * - Must be the last handler
	 * - Prevents exposing internal errors to clients
	 *
	 * Returns:
	 * - HTTP 500 (Internal Server Error)
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {

		// In production, log the exception (e.g., using Logger)
		// log.error("Unexpected error occurred", ex);

		ErrorResponse error = ErrorResponse.builder()
				.timestamp(LocalDateTime.now())
				.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.error("Internal Server Error")
				.message("Something went wrong")
				.build();

		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(error);
	}
}