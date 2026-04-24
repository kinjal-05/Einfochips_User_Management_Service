package userservice.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import userservice.dtos.ApiResponse;
import userservice.dtos.UserResponseDTO;
import userservice.services.GetUserByIdService;

/**
 * REST Controller for User-related operations.
 *
 * <p>
 * Responsibilities:
 * - Handle incoming HTTP requests
 * - Delegate business logic to Service layer
 * - Return standardized HTTP responses
 * </p>
 *
 * <p>
 * NOTE:
 * - All validations are handled using @Valid (Bean Validation)
 * - Exception handling is centralized in GlobalExceptionHandler
 * </p>
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class GetUserByIdController {
	private final GetUserByIdService getUserByIdService;
	/**
	 * Get user by ID.
	 *
	 * @param id User ID
	 * @return User details
	 *
	 * HTTP Status:
	 * - 200 OK
	 * - 404 NOT FOUND if user not found
	 */
	@GetMapping("/getById/{id}")
	public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(
			@PathVariable Long id) {

		UserResponseDTO user = getUserByIdService.getUserById(id);

		return ResponseEntity
				.status(HttpStatus.OK)
				.body(ApiResponse.success(user, "User fetched successfully"));
	}
}
