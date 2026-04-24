package userservice.services;

import userservice.dtos.UserResponseDTO;
import userservice.dtos.UserUpdateRequestDTO;

/**
 * Service interface for User-related business operations.
 *
 * <p>
 * Responsibilities:
 * - Define business logic contracts
 * - Act as a bridge between Controller and Repository layers
 * - Ensure loose coupling (Controller depends on interface, not implementation)
 * </p>
 *
 * <p>
 * NOTE:
 * - Implementation will be provided in UserServiceImpl
 * - All validations, business rules, and security checks should be handled in implementation
 * </p>
 */
public interface UpdateUserService {
	/**
	 * Update user details.
	 *
	 * <p>
	 * Responsibilities:
	 * - Validate input fields
	 * - Check if user exists
	 * - Apply partial updates
	 * </p>
	 *
	 * @param id      User ID
	 * @param request Update request DTO
	 * @return Updated user response DTO
	 */
	UserResponseDTO updateUser(long id, UserUpdateRequestDTO request);
}
