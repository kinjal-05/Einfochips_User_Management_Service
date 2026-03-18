package userservice.dtos;

import java.time.LocalDateTime;

import userservice.enums.Role;

/**
 * DTO (Data Transfer Object) for soft delete response.
 *
 * <p>
 * This record represents the response returned after a user is soft deleted.
 * Instead of permanently removing the record from the database,
 * the system marks it as deleted (isDeleted = true).
 * </p>
 *
 * <p>
 * Why use record?
 * - Immutable and thread-safe
 * - Reduces boilerplate code
 * - Ideal for API response payloads
 * </p>
 *
 * <p>
 * Use Case:
 * - Audit tracking
 * - Restore functionality (if needed in future)
 * - Compliance (data retention policies)
 * </p>
 *
 * @param id           Unique identifier of the user
 * @param email        User email
 * @param role         User role (e.g., ADMIN, USER)
 * @param createdAt    Timestamp when user was created
 * @param updatedAt    Timestamp when user was last updated
 * @param createdById  ID of the user who created this record
 * @param updatedById  ID of the user who last updated this record
 * @param isDeleted    Soft delete flag (true = deleted, false = active)
 * @param message      Status message (e.g., "User deleted successfully")
 */
public record DeleteResponseDTO(
		long id,
		String email,
		Role role,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		long createdById,
		long updatedById,
		boolean isDeleted,
		String message
) {}