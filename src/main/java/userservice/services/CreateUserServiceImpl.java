package userservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import userservice.Utility.MapToUserResponseDTO;
import userservice.dtos.UserRequestDTO;
import userservice.dtos.UserResponseDTO;
import userservice.models.User;
import userservice.repositories.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreateUserServiceImpl implements CreateUserService{
	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;
	private final MapToUserResponseDTO mapToUserResponseDTO;
	/**
	 * Creates a new user with a default temporary password.
	 *
	 * Transactional Behavior:
	 * - Runs within a transaction to ensure user creation is atomic.
	 * - If any failure occurs during save, the transaction is rolled back.
	 *
	 * Notes:
	 * - Password is encoded before persisting.
	 * - Soft delete flag is initialized as false.
	 */
	@Override
	@Transactional
	public UserResponseDTO createUser(UserRequestDTO request) {
		String defaultPassword = "Temp@12345";
		User user = User.builder()
				.email(request.email())
				.password(passwordEncoder.encode(defaultPassword))
				.role(request.role())
				.isDeleted(false)
				.build();
		User savedUser = userRepository.save(user);
		return mapToUserResponseDTO.mapToUserResponseDTO(savedUser);
	}
}
