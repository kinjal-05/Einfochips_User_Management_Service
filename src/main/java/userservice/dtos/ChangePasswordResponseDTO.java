package userservice.dtos;

/**
 * DTO (Data Transfer Object) for password change response.
 *
 * <p>
 * This record is used to send a response after a successful password change.
 * It is immutable and concise, leveraging Java's record feature.
 * </p>
 *
 * <p>
 * Why use record?
 * - Reduces boilerplate (no getters, setters, constructors needed)
 * - Immutable by default (thread-safe)
 * - Ideal for response payloads
 * </p>
 *
 * @param userId  Unique identifier of the user
 * @param email   Email of the user whose password was changed
 * @param message Status message (e.g., "Password updated successfully")
 */
public record ChangePasswordResponseDTO(
		long userId,
		String email,
		String message
) {}