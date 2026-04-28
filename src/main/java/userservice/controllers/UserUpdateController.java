package userservice.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import userservice.config.BaseLogger;
import userservice.dtos.UserApiResponse;
import userservice.dtos.UserResponseDTO;
import userservice.dtos.UserUpdateRequestDTO;
import userservice.services.UserUpdateService;

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
public class UserUpdateController extends BaseLogger {
	private final UserUpdateService updateUserService;

	/**
	 * Update user details.
	 *
	 * @param id      User ID
	 * @param request Fields to update
	 *
	 * @return Updated user details
	 *
	 *         HTTP Status: - 200 OK on success - 404 NOT FOUND if user does not
	 *         exist
	 */
	@PatchMapping("/updateUser/{id}")
	public ResponseEntity<UserApiResponse<UserResponseDTO>> updateUser(@PathVariable Long id,
			@RequestBody UserUpdateRequestDTO request) {

		UserResponseDTO updatedUser = updateUserService.updateUser(id, request);

		return ResponseEntity.status(HttpStatus.OK)
				.body(UserApiResponse.success(updatedUser, "User updated successfully"));
	}
}
