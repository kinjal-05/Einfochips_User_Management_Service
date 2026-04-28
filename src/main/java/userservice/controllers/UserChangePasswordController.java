package userservice.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import userservice.config.BaseLogger;
import userservice.dtos.UserApiResponse;
import userservice.dtos.UserChangePasswordRequestDTO;
import userservice.services.UserChangePasswordService;

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
public class UserChangePasswordController extends BaseLogger {
	private final UserChangePasswordService changePasswordService;

	/**
	 * Change user password.
	 *
	 * <p>
	 * Validates: - old password correctness - new password constraints
	 * </p>
	 *
	 * @param request Password change request
	 * @return Password change confirmation
	 *
	 *         HTTP Status: - 200 OK - 400 BAD REQUEST (validation errors) - 401
	 *         UNAUTHORIZED (wrong old password)
	 */
	@PatchMapping("/changePassword")
	public ResponseEntity<UserApiResponse<Void>> changePassword(
			@Valid @RequestBody UserChangePasswordRequestDTO request) {

		changePasswordService.changePassword(request);

		return ResponseEntity.status(HttpStatus.OK)
				.body(UserApiResponse.success(null, "Password changed successfully"));
	}
}
