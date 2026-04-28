package userservice.config;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import userservice.dtos.UserApiResponse;
import userservice.exceptions.ResourceNotFoundException;

/**
 * Global exception handler for the application.
 *
 * <p>
 * This class is annotated with {@code @RestControllerAdvice}, enabling
 * centralized exception handling across all {@code @RestController} components.
 * It ensures consistent and structured API error responses using the
 * {@link userservice.dtos.UserApiResponse} wrapper.
 * </p>
 *
 * <p>
 * The handler intercepts specific exceptions and maps them to appropriate HTTP
 * status codes along with meaningful error messages.
 * </p>
 *
 * <p>
 * <b>Handled Exceptions:</b>
 * </p>
 * <ul>
 * <li>{@link org.springframework.web.bind.MethodArgumentNotValidException} -
 * Triggered during validation failures of request bodies annotated with
 * {@code @Valid}. Returns {@code 400 BAD REQUEST} with aggregated field-level
 * error messages.</li>
 * <li>
 * {@link org.springframework.security.authentication.BadCredentialsException} -
 * Triggered when authentication fails due to invalid credentials. Returns
 * {@code 401 UNAUTHORIZED} with a generic error message.</li>
 * <li>{@link userservice.exceptions.ResourceNotFoundException} - Thrown when a
 * requested resource is not found. Returns {@code 404 NOT FOUND} with the
 * exception message.</li>
 * <li>{@link java.lang.Exception} - A fallback handler for all unhandled
 * exceptions. Returns {@code 500 INTERNAL SERVER ERROR} with a generic message.
 * </li>
 * </ul>
 *
 * <p>
 * This approach improves maintainability by separating error handling logic
 * from business logic and ensures a uniform API response structure across the
 * application.
 * </p>
 *
 * <p>
 * Note:
 * <ul>
 * <li>Avoid exposing sensitive internal error details in production.</li>
 * <li>Consider adding logging (e.g., using SLF4J) for debugging and monitoring
 * purposes.</li>
 * <li>Additional handlers (e.g., for {@code DataIntegrityViolationException})
 * can be added as needed to handle database-related errors explicitly.</li>
 * </ul>
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Handles validation failures triggered by @Valid or @Validated annotations in
	 * controller request bodies or parameters.
	 *
	 * <p>
	 * This method captures {@link MethodArgumentNotValidException} and extracts all
	 * field-level validation errors from the . Each error is formatted as:
	 * 
	 * <pre>
	 *     fieldName: errorMessage
	 * </pre>
	 *
	 * <p>
	 * All field error messages are concatenated into a single comma-separated
	 * string to provide a concise and user-friendly response.
	 *
	 * <p>
	 * Example response:
	 * 
	 * <pre>
	 * {
	 *   "success": false,
	 *   "message": "email: must be a valid email, password: must not be blank",
	 *   "data": null,
	 *   "timestamp": "2026-04-24T12:00:00"
	 * }
	 * </pre>
	 *
	 * @param ex the exception thrown when validation on an argument fails
	 * @return ResponseEntity containing a standardized ApiResponse with HTTP 400
	 *         (Bad Request) status and aggregated validation error message
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<UserApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {

		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage()).collect(Collectors.joining(", "));

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(UserApiResponse.failure(message));
	}

	/**
	 * Handles authentication failures caused by invalid credentials.
	 *
	 * <p>
	 * This method captures {@link BadCredentialsException} typically thrown during
	 * login attempts when the provided email/username or password is incorrect. For
	 * security reasons, it does not expose specific details about which field
	 * caused the failure and instead returns a generic error message.
	 *
	 * <p>
	 * This helps prevent user enumeration and protects sensitive authentication
	 * logic.
	 *
	 * <p>
	 * Example response:
	 * 
	 * <pre>
	 * {
	 *   "success": false,
	 *   "message": "Invalid email or password",
	 *   "data": null,
	 *   "timestamp": "2026-04-24T12:00:00"
	 * }
	 * </pre>
	 *
	 * @param ex the exception thrown when authentication fails due to invalid
	 *           credentials
	 * @return ResponseEntity containing a standardized ApiResponse with HTTP 401
	 *         (Unauthorized) status and a generic authentication error message
	 */
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<UserApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(UserApiResponse.failure("Invalid email or password"));
	}

	/**
	 * Handles cases where a requested resource is not found.
	 *
	 * <p>
	 * This method captures {@link ResourceNotFoundException}, which is typically
	 * thrown when an entity (e.g., User, Order, etc.) does not exist in the system
	 * for the given identifier or search criteria.
	 *
	 * <p>
	 * The exception message is returned directly in the response to provide
	 * meaningful context about what resource was not found.
	 *
	 * <p>
	 * Example response:
	 * 
	 * <pre>
	 * {
	 *   "success": false,
	 *   "message": "User not found with id: 101",
	 *   "data": null,
	 *   "timestamp": "2026-04-24T12:00:00"
	 * }
	 * </pre>
	 *
	 * @param ex the exception thrown when a requested resource is not found
	 * @return ResponseEntity containing a standardized ApiResponse with HTTP 404
	 *         (Not Found) status and a descriptive error message
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<UserApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(UserApiResponse.failure(ex.getMessage()));
	}

	/**
	 * Handles all unhandled and unexpected exceptions across the application.
	 *
	 * <p>
	 * This method acts as a global fallback handler for any exception that is not
	 * explicitly handled by more specific {@code @ExceptionHandler} methods. It
	 * ensures that the application does not expose internal implementation details
	 * or sensitive information to the client.
	 *
	 * <p>
	 * Instead of returning the actual exception message, a generic error message is
	 * sent in the response for security and consistency purposes.
	 *
	 * <p>
	 * <b>Note:</b> The actual exception should be logged internally (e.g., using a
	 * logger) for debugging and monitoring, even though it is not exposed to the
	 * client.
	 *
	 * <p>
	 * Example response:
	 * 
	 * <pre>
	 * {
	 *   "success": false,
	 *   "message": "Something went wrong",
	 *   "data": null,
	 *   "timestamp": "2026-04-24T12:00:00"
	 * }
	 * </pre>
	 *
	 * @param ex the unexpected exception thrown during request processing
	 * @return ResponseEntity containing a standardized ApiResponse with HTTP 500
	 *         (Internal Server Error) status and a generic error message
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<UserApiResponse<Object>> handleGeneric(Exception ex) {

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(UserApiResponse.failure("Something went wrong"));
	}
}