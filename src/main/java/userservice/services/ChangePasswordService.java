package userservice.services;

import userservice.dtos.ChangePasswordRequestDTO;
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
public interface ChangePasswordService {
	/**
	 * Change user password.
	 *
	 * <p>
	 * Responsibilities:
	 * - Validate old password
	 * - Encrypt new password
	 * - Update securely in database
	 * </p>
	 *
	 * @param request Password change request DTO
	 * @return Password change response DTO
	 */
	void changePassword(ChangePasswordRequestDTO request);
}
