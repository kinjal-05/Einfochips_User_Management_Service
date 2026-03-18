package userservice.dtos;

import userservice.enums.Role;

import jakarta.validation.constraints.Email;

/**
 * DTO (Data Transfer Object) for updating user details.
 *
 * <p>
 * This record represents the request payload for updating an existing user.
 * It supports partial updates, meaning:
 * - Only non-null fields will be updated
 * - Null fields will be ignored
 * </p>
 *
 * <p>
 * Validation:
 * - Email must follow valid format if provided
 * - Role is optional but must be valid if present
 * </p>
 *
 * <p>
 * Why use record?
 * - Immutable by default
 * - Reduces boilerplate code
 * - Ideal for request payloads
 * </p>
 */
public record UserUpdateRequestDTO(

		/**
		 * Updated email address.
		 *
		 * Constraints:
		 * - Must follow valid email format (if provided)
		 * - Can be null (no update)
		 */
		@Email(message = "Invalid Email Format")
		String email,

		/**
		 * Updated user role.
		 *
		 * Example:
		 * - ADMIN
		 * - USER
		 *
		 * NOTE:
		 * - Optional field
		 * - If null, role will not be updated
		 */
		Role role

) {}