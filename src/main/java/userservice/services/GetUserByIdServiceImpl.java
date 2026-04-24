package userservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import userservice.Utility.GetActiveUser;
import userservice.Utility.MapToUserResponseDTO;
import userservice.dtos.UserResponseDTO;
import userservice.exceptions.ResourceNotFoundException;
import userservice.models.User;
import userservice.repositories.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class GetUserByIdServiceImpl implements  GetUserByIdService{
	private final UserRepository userRepository;
	private final GetActiveUser getActiveUser;
	private final MapToUserResponseDTO mapToUserResponseDTO;
	/**
	 * Fetches a user by ID.
	 *
	 * Transactional Behavior:
	 * - Read-only transaction ensures no accidental modifications.
	 *
	 * Notes:
	 * - Only active (non-deleted) users are returned.
	 */
	@Override
	@Transactional(readOnly = true)
	public UserResponseDTO getUserById(long id) {
		User user =getActiveUser.getUserOrThrow(id);
		return mapToUserResponseDTO.mapToUserResponseDTO(user);
	}
}
