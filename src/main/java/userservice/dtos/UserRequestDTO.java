package userservice.dtos;

import userservice.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO (Data Transfer Object) for user registration request.
 *
 * <p>
 * This record represents the input payload required to create a new user.
 * It uses Bean Validation annotations to enforce input constraints.
 * </p>
 *
 * <p>
 * Why use record?
 * - Immutable by default
 * - Reduces boilerplate code
 * - Ideal for request/response models
 * </p>
 *
 * <p>
 * Validation:
 * - Ensures only valid and complete data reaches the service layer
 * - Automatically triggers MethodArgumentNotValidException on failure
 * </p>
 */
public record UserRequestDTO(

		/**
		 * User email address.
		 *
		 * Constraints:
		 * - Must not be blank
		 * - Must follow valid email format
		 */
		@Email(message = "Invalid Email Format")
		@NotBlank(message = "Email is Required")
		String email,

		/**
		 * User role.
		 *
		 * Constraints:
		 * - Must not be null
		 * - Defines user authorization level (e.g., ADMIN, USER)
		 */
		@NotNull(message = "Role is required")
		Role role

) {}