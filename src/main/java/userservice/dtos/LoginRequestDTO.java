package userservice.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/*
 * FIX: Added @NotBlank and @Email so @Valid on the controller
 * can trigger MethodArgumentNotValidException → 400 BAD REQUEST.
 *
 * Without these annotations, passing blank email/password
 * would reach the service layer with no validation error,
 * so the "400 when blank" tests could never pass.
 */
public record LoginRequestDTO(

		@NotBlank(message = "Email is required")
		@Email(message = "Invalid email format")
		String email,

		@NotBlank(message = "Password is required")
		String password

) {}