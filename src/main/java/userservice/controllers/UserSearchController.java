package userservice.controllers;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import userservice.dtos.UserApiResponse;
import userservice.dtos.UserResponseDTO;
import userservice.dtos.UserSearchRequestDTO;
import userservice.enums.Role;
import userservice.services.UserSearchService;

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
public class UserSearchController {
	private final UserSearchService searchUserService;

	/**
	 * Search users with dynamic filtering and pagination.
	 *
	 * <p>
	 * Supports filtering by: - email (partial match) - role - createdBy, updatedBy
	 * - date range
	 * </p>
	 *
	 *
	 * 
	 * @return Paginated list of users
	 */
	@GetMapping("/search")
	public ResponseEntity<UserApiResponse<Page<UserResponseDTO>>> searchUsers(
			@RequestParam(required = false) String email, @RequestParam(required = false) Role role,
			@RequestParam(required = false) Long createdById, @RequestParam(required = false) Long updatedById,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
			Pageable pageable) {

		UserSearchRequestDTO request = new UserSearchRequestDTO(email, role, createdById, updatedById, fromDate,
				toDate);

		Page<UserResponseDTO> response = searchUserService.searchUsers(request, pageable);

		return ResponseEntity.status(HttpStatus.OK)
				.body(UserApiResponse.success(response, "Users fetched successfully"));
	}
}
