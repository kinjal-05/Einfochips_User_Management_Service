package userservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import userservice.dtos.*;
import userservice.enums.Role;
import userservice.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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
public class UserController {

	// Service layer dependency (business logic)
	private final UserService userService;

	// PasswordEncoder injected (generally used in service, but kept here if needed)
	private final PasswordEncoder passwordEncoder;

	/**
	 * Register a new user.
	 *
	 * @param request User registration details (validated using @Valid)
	 * @return Created user response
	 *
	 * HTTP Status:
	 * - 201 CREATED on success
	 * - 400 BAD REQUEST if validation fails
	 */
	@PostMapping("/registerUser")
	public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(
			@RequestBody @Valid UserRequestDTO request) {

		UserResponseDTO response = userService.createUser(request);

		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(ApiResponse.success(response, "User created successfully"));
	}

	/**
	 * Authenticate user (Login).
	 *
	 * IMPORTANT:
	 * - Uses @Valid (NOT @Validated) to trigger MethodArgumentNotValidException
	 * - Ensures invalid input returns HTTP 400 instead of 500
	 *
	 * @param request Login credentials
	 * @return JWT token or login response
	 *
	 * HTTP Status:
	 * - 200 OK on success
	 * - 400 BAD REQUEST (validation errors)
	 * - 401 UNAUTHORIZED (invalid credentials)
	 */
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
			@Valid @RequestBody LoginRequestDTO request) {

		log.info("{}","Enter in Login Controller");
		LoginResponseDTO response = userService.login(request);

		return ResponseEntity
				.status(HttpStatus.OK)
				.body(ApiResponse.success(response, "Login successful"));
	}

	/**
	 * Search users with dynamic filtering and pagination.
	 *
	 * <p>
	 * Supports filtering by:
	 * - email (partial match)
	 * - role
	 * - createdBy, updatedBy
	 * - date range
	 * </p>
	 *

	 *
	 * @return Paginated list of users
	 */
	@GetMapping("/search")
	public ResponseEntity<ApiResponse<Page<UserResponseDTO>>> searchUsers(
			@RequestParam(required = false) String email,
			@RequestParam(required = false) Role role,
			@RequestParam(required = false) Long createdById,
			@RequestParam(required = false) Long updatedById,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			LocalDateTime fromDate,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			LocalDateTime toDate,
			Pageable pageable) {

		log.info("Enter in Controller Part");
		log.info("Params: email={}, role={}, createdById={}, updatedById={}",
				email, role, createdById, updatedById);
		UserSearchRequestDTO request = new UserSearchRequestDTO(
				email, role, createdById, updatedById, fromDate, toDate
		);
		log.info("{}",request);
		Page<UserResponseDTO> response = userService.searchUsers(request, pageable);

		return ResponseEntity
				.status(HttpStatus.OK)
				.body(ApiResponse.success(response, "Users fetched successfully"));
	}

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

		UserResponseDTO updatedUser = userService.updateUser(id, request);

		return ResponseEntity
				.status(HttpStatus.OK)
				.body(ApiResponse.success(updatedUser, "User updated successfully"));
	}

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

		UserResponseDTO user = userService.getUserById(id);

		return ResponseEntity
				.status(HttpStatus.OK)
				.body(ApiResponse.success(user, "User fetched successfully"));
	}

	/**
	 * Soft delete a user.
	 *
	 * <p>
	 * Instead of permanently deleting, marks the user as inactive/deleted.
	 * Useful for audit and recovery.
	 * </p>
	 *
	 * @param id User ID
	 * @return Deletion confirmation response
	 *
	 * HTTP Status:
	 * - 200 OK
	 * - 404 NOT FOUND if user does not exist
	 */
	@DeleteMapping("/deleteUser/{id}")
	public ResponseEntity<ApiResponse<Void>> softDeleteUser(@PathVariable long id) {

		userService.softDeleteUser(id);

		return ResponseEntity
				.status(HttpStatus.OK)
				.body(ApiResponse.success(null, "User deleted successfully"));
	}

	/**
	 * Change user password.
	 *
	 * <p>
	 * Validates:
	 * - old password correctness
	 * - new password constraints
	 * </p>
	 *
	 * @param request Password change request
	 * @return Password change confirmation
	 *
	 * HTTP Status:
	 * - 200 OK
	 * - 400 BAD REQUEST (validation errors)
	 * - 401 UNAUTHORIZED (wrong old password)
	 */
	@PatchMapping("/changePassword")
	public ResponseEntity<ApiResponse<Void>> changePassword(
			@Valid @RequestBody ChangePasswordRequestDTO request) {

		userService.changePassword(request);

		return ResponseEntity
				.status(HttpStatus.OK)
				.body(ApiResponse.success(null, "Password changed successfully"));
	}


}