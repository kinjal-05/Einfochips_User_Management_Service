package userservice.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import userservice.dtos.*;

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
public interface UserService {

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
	UserResponseDTO registerUser(UserRequestDTO request);

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
	LoginResponseDTO login(LoginRequestDTO request);

	/**
	 * Search users with dynamic filters and pagination.
	 *
	 * <p>
	 * Supports:
	 * - Email search (partial match)
	 * - Role filtering
	 * - Created/updated user filters
	 * - Date range filtering
	 * </p>
	 *
	 * @param request   Search filter criteria
	 * @param pageable  Pagination and sorting information
	 * @return Paginated list of users
	 */
	Page<UserResponseDTO> searchUsers(UserSearchRequestDTO request, Pageable pageable);

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

	/**
	 * Fetch user by ID.
	 *
	 * <p>
	 * Responsibilities:
	 * - Retrieve user from database
	 * - Throw exception if not found
	 * </p>
	 *
	 * @param id User ID
	 * @return User response DTO
	 */
	UserResponseDTO getUserById(long id);

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
	DeleteResponseDTO softDeleteUser(long id);

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
	ChangePasswordResponseDTO changePassword(ChangePasswordRequestDTO request);
}