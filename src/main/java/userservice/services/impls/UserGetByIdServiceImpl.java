package userservice.services.impls;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import userservice.dtos.UserResponseDTO;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.services.UserGetByIdService;
import userservice.utility.GetActiveUser;
import userservice.utility.MapToUserResponseDTO;

@Service
@RequiredArgsConstructor
public class UserGetByIdServiceImpl implements UserGetByIdService {
	private final UserRepository userRepository;
	private final GetActiveUser getActiveUser;
	private final MapToUserResponseDTO mapToUserResponseDTO;

	/**
	 * Fetches a user by ID.
	 *
	 * Transactional Behavior: - Read-only transaction ensures no accidental
	 * modifications.
	 *
	 * Notes: - Only active (non-deleted) users are returned.
	 */
	@Override
	@Transactional(readOnly = true)
	public UserResponseDTO getUserById(long id) {
		User user = getActiveUser.getUserOrThrow(id);
		return mapToUserResponseDTO.mapToUserResponseDTO(user);
	}
}
