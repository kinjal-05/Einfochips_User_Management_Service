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