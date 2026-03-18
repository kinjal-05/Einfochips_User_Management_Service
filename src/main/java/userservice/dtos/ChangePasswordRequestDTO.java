package userservice.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
 * FIX: Added @NotBlank so @Valid on the controller
 * triggers MethodArgumentNotValidException → 400 BAD REQUEST.
 *
 * Without these annotations, passing blank oldPassword/newPassword
 * would reach the service with no error — "400 when blank" tests fail.
 */
public record ChangePasswordRequestDTO(

		@NotBlank(message = "Old password is required")
		String oldPassword,

		@NotBlank(message = "New password is required")
		@Size(min = 8, message = "New password must be at least 8 characters")
		String newPassword

) {}