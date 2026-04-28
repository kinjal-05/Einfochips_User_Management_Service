package userservice.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import userservice.config.BaseLogger;
import userservice.dtos.UserApiResponse;
import userservice.dtos.UserLoginRequestDTO;
import userservice.dtos.UserLoginResponseDTO;
import userservice.services.UserLoginService;

/**
 * REST Controller for User-related operations.
 *
 * <p>
 * Responsibilities: - Handle incoming HTTP requests - Delegate business logic
 * to Service layer - Return standardized HTTP responses
 * </p>
 *
 * <p>
 * NOTE: - All validations are handled using @Valid (Bean Validation) -
 * Exception handling is centralized in GlobalExceptionHandler
 * </p>
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserLoginController extends BaseLogger {
	private final UserLoginService loginUserService;

	/**
	 * Authenticate user (Login).
	 *
	 * IMPORTANT: - Uses @Valid (NOT @Validated) to trigger
	 * MethodArgumentNotValidException - Ensures invalid input returns HTTP 400
	 * instead of 500
	 *
	 * @param request Login credentials
	 * @return JWT token or login response
	 *
	 *         HTTP Status: - 200 OK on success - 400 BAD REQUEST (validation
	 *         errors) - 401 UNAUTHORIZED (invalid credentials)
	 */
	@PostMapping("/login")
	public ResponseEntity<UserApiResponse<UserLoginResponseDTO>> login(
			@Valid @RequestBody UserLoginRequestDTO request) {

		log.info("{}", "Enter in Login Controller");
		UserLoginResponseDTO response = loginUserService.login(request);

		return ResponseEntity.status(HttpStatus.OK).body(UserApiResponse.success(response, "Login successful"));
	}
}
