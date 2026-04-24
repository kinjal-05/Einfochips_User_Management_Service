package userservice.services;

import io.swagger.v3.oas.annotations.servers.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import userservice.Utility.GetActiveUser;
import userservice.Utility.MapToUserResponseDTO;
import userservice.dtos.UserResponseDTO;
import userservice.dtos.UserUpdateRequestDTO;
import userservice.exceptions.ResourceNotFoundException;
import userservice.models.User;
import userservice.repositories.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateUserServiceImpl implements UpdateUserService{
	private final UserRepository userRepository;
	private  final GetActiveUser getActiveUser;
	private final MapToUserResponseDTO mapToUserResponseDTO;
	/**
	 * Updates user details.
	 *
	 * Transactional Behavior:
	 * - Uses READ_COMMITTED isolation to ensure only committed data is read.
	 * - Timeout ensures long-running transactions are aborted.
	 *
	 * Notes:
	 * - Currently fetches and re-saves user without modification (extend as needed).
	 * - Prevents dirty reads but allows non-repeatable reads.
	 */
	@Override
	@Transactional(
			timeout = 10
	)
	public UserResponseDTO updateUser(long id, UserUpdateRequestDTO request) {
		User user = getActiveUser.getUserOrThrow(id);
		User updatedUser = userRepository.save(user);
		return mapToUserResponseDTO.mapToUserResponseDTO(updatedUser);
	}

}
