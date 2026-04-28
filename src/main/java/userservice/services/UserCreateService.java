package userservice.services;

import userservice.dtos.UserRequestDTO;
import userservice.dtos.UserResponseDTO;
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
public interface UserCreateService {
	/**
	 * Register a new user.
	 *
	 * <p>
	 * Responsibilities:
	 * - Validate request data
	 * - Check for duplicate email
	 * - Encode password
	 * - Save user to database
	 * </p>
	 *
	 * @param request User registration request DTO
	 * @return Created user response DTO
	 */
	UserResponseDTO createUser(UserRequestDTO request);
}
