package userservice.dtos;

import userservice.enums.Role;

import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) for user search/filter request.
 *
 * <p>
 * This record is used to pass dynamic filtering criteria
 * for searching users in the system.
 * </p>
 *
 * <p>
 * All fields are optional:
 * - Only non-null values will be used for filtering
 * - Enables flexible and dynamic queries
 * </p>
 *
 * <p>
 * Typically used with:
 * - Spring Data JPA Specifications
 * - Pagination (Pageable)
 * </p>
 *
 * <p>
 * Example Use Cases:
 * - Admin dashboard search
 * - Audit filtering
 * - Role-based user listing
 * </p>
 */
public record UserSearchRequestDTO(

		/**
		 * Email filter (supports partial match).
		 *
		 * Example:
		 * - Input: "kinjal"
		 * - Matches: kinjal@gmail.com, testkinjal@yahoo.com
		 */
		String email,

		/**
		 * Filter by user role.
		 *
		 * Example:
		 * - ADMIN
		 * - USER
		 */
		Role role,

		/**
		 * Filter by creator user ID.
		 *
		 * NOTE:
		 * - Should ideally be Long instead of primitive long
		 *   to allow null (optional filtering)
		 */
		long createdById,

		/**
		 * Filter by last updater user ID.
		 *
		 * NOTE:
		 * - Should ideally be Long instead of primitive long
		 */
		long updatedById,

		/**
		 * Filter users created after or equal to this date.
		 *
		 * Example:
		 * - fromDate = 2026-01-01 → fetch users created after Jan 1
		 */
		LocalDateTime fromDate,

		/**
		 * Filter users created before or equal to this date.
		 *
		 * Example:
		 * - toDate = 2026-12-31 → fetch users created before Dec 31
		 */
		LocalDateTime toDate

) {}