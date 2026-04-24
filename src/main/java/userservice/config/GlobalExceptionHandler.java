package userservice.config;
import userservice.dtos.ApiResponse;
import userservice.exceptions.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.stream.Collectors;
/**
 * Global exception handler for the application.
 *
 * <p>
 * This class is annotated with {@code @RestControllerAdvice}, enabling centralized
 * exception handling across all {@code @RestController} components. It ensures
 * consistent and structured API error responses using the {@link userservice.dtos.ApiResponse} wrapper.
 * </p>
 *
 * <p>
 * The handler intercepts specific exceptions and maps them to appropriate
 * HTTP status codes along with meaningful error messages.
 * </p>
 *
 * <p><b>Handled Exceptions:</b></p>
 * <ul>
 *   <li>
 *     {@link org.springframework.web.bind.MethodArgumentNotValidException} -
 *     Triggered during validation failures of request bodies annotated with {@code @Valid}.
 *     Returns {@code 400 BAD REQUEST} with aggregated field-level error messages.
 *   </li>
 *   <li>
 *     {@link org.springframework.security.authentication.BadCredentialsException} -
 *     Triggered when authentication fails due to invalid credentials.
 *     Returns {@code 401 UNAUTHORIZED} with a generic error message.
 *   </li>
 *   <li>
 *     {@link userservice.exceptions.ResourceNotFoundException} -
 *     Thrown when a requested resource is not found.
 *     Returns {@code 404 NOT FOUND} with the exception message.
 *   </li>
 *   <li>
 *     {@link java.lang.Exception} -
 *     A fallback handler for all unhandled exceptions.
 *     Returns {@code 500 INTERNAL SERVER ERROR} with a generic message.
 *   </li>
 * </ul>
 *
 * <p>
 * This approach improves maintainability by separating error handling logic
 * from business logic and ensures a uniform API response structure across the application.
 * </p>
 *
 * <p>
 * Note:
 * <ul>
 *   <li>Avoid exposing sensitive internal error details in production.</li>
 *   <li>Consider adding logging (e.g., using SLF4J) for debugging and monitoring purposes.</li>
 *   <li>Additional handlers (e.g., for {@code DataIntegrityViolationException}) can be added
 *       as needed to handle database-related errors explicitly.</li>
 * </ul>
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Object>> handleValidation(
			MethodArgumentNotValidException ex) {

		String message = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.failure(message));
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiResponse<Object>> handleBadCredentials(
			BadCredentialsException ex) {

		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.failure("Invalid email or password"));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiResponse<Object>> handleNotFound(
			ResourceNotFoundException ex) {

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.failure(ex.getMessage()));
	}


	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {

		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.failure("Something went wrong"));
	}
}