package userservice.services;

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
public interface SoftDeleteUserService {
	/**
	 * Soft delete a user.
	 *
	 * <p>
	 * Instead of deleting the record permanently:
	 * - Marks user as deleted (isDeleted = true)
	 * - Preserves data for auditing and recovery
	 * </p>
	 *
	 * @param id User ID
	 * @return Deletion response DTO
	 */
	void softDeleteUser(long id);
}
