package userservice.dtos;

import userservice.enums.Role;

/**
 * DTO (Data Transfer Object) for login response.
 *
 * <p>
 * This record is returned after successful user authentication.
 * It contains essential user details along with an authentication token.
 * </p>
 *
 * <p>
 * Why use record?
 * - Immutable by default (safer for multi-threaded environments)
 * - Reduces boilerplate code
 * - Ideal for API response payloads
 * </p>
 *
 * <p>
 * Typical Usage:
 * - Returned after successful login
 * - Used by frontend to store authentication token (e.g., JWT)
 * - Token is included in subsequent API requests for authorization
 * </p>
 *
 * @param id      Unique identifier of the user
 * @param email   Authenticated user's email
 * @param role    User role (e.g., ADMIN, USER)
 * @param token   Authentication token (e.g., JWT)
 * @param message Status message (e.g., "Login successful")
 */
public record UserLoginResponseDTO(
		long id,
		String email,
		Role role,
		String token,
		String message
) {}