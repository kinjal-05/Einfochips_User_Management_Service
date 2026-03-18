package userservice.dtos;

import java.time.LocalDateTime;

import userservice.enums.Role;

/**
 * DTO (Data Transfer Object) for user response.
 *
 * <p>
 * This record represents the data returned to the client
 * when fetching user details (e.g., getById, register, update).
 * </p>
 *
 * <p>
 * It exposes only necessary fields and hides sensitive data
 * such as passwords or internal flags.
 * </p>
 *
 * <p>
 * Why use record?
 * - Immutable and thread-safe
 * - Reduces boilerplate code
 * - Ideal for API responses
 * </p>
 *
 * <p>
 * Use Cases:
 * - User profile response
 * - Admin user listing
 * - Audit-related data display
 * </p>
 *
 * @param id           Unique identifier of the user
 * @param email        User email
 * @param role         User role (e.g., ADMIN, USER)
 * @param createdAt    Timestamp when the user was created
 * @param updatedAt    Timestamp when the user was last updated
 * @param createdById  ID of the user who created this record
 * @param updatedById  ID of the user who last updated this record
 */
public record UserResponseDTO(
		long id,
		String email,
		Role role,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		long createdById,
		long updatedById
) {}