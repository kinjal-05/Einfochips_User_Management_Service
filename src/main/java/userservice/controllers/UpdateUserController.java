package userservice.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import userservice.dtos.ApiResponse;
import userservice.dtos.UserResponseDTO;
import userservice.dtos.UserUpdateRequestDTO;
import userservice.services.UpdateUserService;

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
public class UpdateUserController {
	private final UpdateUserService updateUserService;
	/**
	 * Update user details.
	 *
	 * @param id      User ID
	 * @param request Fields to update
	 *
	 * @return Updated user details
	 *
	 * HTTP Status:
	 * - 200 OK on success
	 * - 404 NOT FOUND if user does not exist
	 */
	@PatchMapping("/updateUser/{id}")
	public ResponseEntity<ApiResponse<UserResponseDTO>> updateUser(
			@PathVariable Long id,
			@RequestBody UserUpdateRequestDTO request) {

		UserResponseDTO updatedUser = updateUserService.updateUser(id, request);

		return ResponseEntity
				.status(HttpStatus.OK)
				.body(ApiResponse.success(updatedUser, "User updated successfully"));
	}
}
