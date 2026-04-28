package userservice.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import userservice.dtos.UserApiResponse;
import userservice.services.UserSoftDeleteService;

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
public class UserSoftDeleteController {
	private final UserSoftDeleteService softDeleteUserService;

	/**
	 * Soft delete a user.
	 *
	 * <p>
	 * Instead of permanently deleting, marks the user as inactive/deleted. Useful
	 * for audit and recovery.
	 * </p>
	 *
	 * @param id User ID
	 * @return Deletion confirmation response
	 *
	 *         HTTP Status: - 200 OK - 404 NOT FOUND if user does not exist
	 */
	@DeleteMapping("/deleteUser/{id}")
	public ResponseEntity<UserApiResponse<Void>> softDeleteUser(@PathVariable long id) {

		softDeleteUserService.softDeleteUser(id);

		return ResponseEntity.status(HttpStatus.OK).body(UserApiResponse.success(null, "User deleted successfully"));
	}
}
