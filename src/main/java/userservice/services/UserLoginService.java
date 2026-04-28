package userservice.services;

import userservice.dtos.UserLoginRequestDTO;
import userservice.dtos.UserLoginResponseDTO;
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
public interface UserLoginService {
	/**
	 * Authenticate user (login).
	 *
	 * <p>
	 * Responsibilities:
	 * - Validate email and password
	 * - Authenticate using Spring Security
	 * - Generate token (JWT or session-based)
	 * </p>
	 *
	 * @param request Login request DTO
	 * @return Login response (e.g., token, user info)
	 */
	UserLoginResponseDTO login(UserLoginRequestDTO request);
}
