package userservice.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import userservice.dtos.UserResponseDTO;
import userservice.dtos.UserSearchRequestDTO;
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
public interface UserSearchService {
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
}
