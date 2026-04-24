package userservice.services;

import io.swagger.v3.oas.annotations.servers.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import userservice.Utility.MapToUserResponseDTO;
import userservice.config.UserSpecification;
import userservice.dtos.UserResponseDTO;
import userservice.dtos.UserSearchRequestDTO;
import userservice.models.User;
import userservice.repositories.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchUserServiceImpl implements SearchUserService{
	private final UserRepository userRepository;
	private final MapToUserResponseDTO mapToUserResponseDTO;
	/**
	 * Searches users based on dynamic filtering criteria.
	 *
	 * Transactional Behavior:
	 * - Read-only transaction for performance optimization.
	 * - Prevents accidental writes and reduces DB overhead.
	 *
	 * Notes:
	 * - Uses JPA Specification for flexible filtering.
	 * - Supports pagination via Pageable.
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<UserResponseDTO> searchUsers(UserSearchRequestDTO request, Pageable pageable) {
		Specification<User> specification = UserSpecification.filterUsers(
				request.email(),
				request.role(),
				request.createdById(),
				request.updatedById(),
				request.fromDate(),
				request.toDate()
		);
		Page<User> usersPage = userRepository.findAll(specification, pageable);
		return usersPage.map(mapToUserResponseDTO::mapToUserResponseDTO);
	}


}
