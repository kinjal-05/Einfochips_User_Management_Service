package userservice.services.impls;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import userservice.config.BaseLogger;
import userservice.config.UserSpecification;
import userservice.dtos.UserResponseDTO;
import userservice.dtos.UserSearchRequestDTO;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.services.UserSearchService;
import userservice.utility.MapToUserResponseDTO;

/**
 * Service implementation responsible for searching users using dynamic
 * filtering criteria with pagination support.
 *
 * <p>
 * This service enables flexible user retrieval based on optional search
 * parameters such as:
 * <ul>
 * <li>Email address</li>
 * <li>User role</li>
 * <li>Created by user ID</li>
 * <li>Updated by user ID</li>
 * <li>Date range filters</li>
 * </ul>
 *
 * <p>
 * Filtering is implemented using Spring Data JPA {@link Specification} for
 * scalable and maintainable query building.
 *
 * <p>
 * Results are returned in paginated format and mapped to
 * {@link UserResponseDTO} objects.
 *
 * <p>
 * This service uses a read-only transaction for performance optimization and to
 * prevent unintended write operations.
 *
 * @author Kinjal Mistry
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class UserSearchServiceImpl extends BaseLogger implements UserSearchService {
	private final UserRepository userRepository;
	private final MapToUserResponseDTO mapToUserResponseDTO;

	/**
	 * Searches users using dynamic filters and pagination.
	 *
	 * <p>
	 * Supported filters are optional and applied only when provided. If no filters
	 * are supplied, all matching users are returned according to pagination
	 * settings.
	 *
	 * <p>
	 * Execution flow:
	 * <ul>
	 * <li>Build dynamic JPA specification from request filters</li>
	 * <li>Execute paginated database query</li>
	 * <li>Map entity results to response DTOs</li>
	 * </ul>
	 *
	 * @param request  request containing optional search criteria
	 * @param pageable pagination and sorting configuration
	 * @return paginated list of matching user response DTOs
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<UserResponseDTO> searchUsers(UserSearchRequestDTO request, Pageable pageable) {
		Specification<User> specification = UserSpecification.filterUsers(request.email(), request.role(),
				request.createdById(), request.updatedById(), request.fromDate(), request.toDate());
		Page<User> usersPage = userRepository.findAll(specification, pageable);
		return usersPage.map(mapToUserResponseDTO::mapToUserResponseDTO);
	}

}
