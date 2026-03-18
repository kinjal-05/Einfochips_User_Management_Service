package userservice.controllers;

import userservice.dtos.*;
import userservice.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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
	public ResponseEntity<UserResponseDTO> registerUser(
			@RequestBody @Valid UserRequestDTO request) {

		UserResponseDTO response = userService.registerUser(request);

		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(response);
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
	public ResponseEntity<LoginResponseDTO> login(
			@Valid @RequestBody LoginRequestDTO request) {

		return ResponseEntity.ok(userService.login(request));
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
	 * @param request   Filter criteria
	 * @param pageable  Pagination and sorting information
	 *
	 * @return Paginated list of users
	 */
	@PostMapping("/search")
	public Page<UserResponseDTO> searchUsers(
			@RequestBody UserSearchRequestDTO request,
			Pageable pageable) {

		return userService.searchUsers(request, pageable);
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
	public ResponseEntity<UserResponseDTO> updateUser(
			@PathVariable Long id,
			@RequestBody UserUpdateRequestDTO request) {

		UserResponseDTO updatedUser = userService.updateUser(id, request);

		return ResponseEntity.ok(updatedUser);
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
	public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {

		UserResponseDTO user = userService.getUserById(id);

		return ResponseEntity.ok(user);
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
	public ResponseEntity<DeleteResponseDTO> softDeleteUser(@PathVariable Long id) {

		DeleteResponseDTO response = userService.softDeleteUser(id);

		return ResponseEntity.ok(response);
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
	public ResponseEntity<ChangePasswordResponseDTO> changePassword(
			@RequestBody @Valid ChangePasswordRequestDTO request) {

		return ResponseEntity.ok(userService.changePassword(request));
	}
}