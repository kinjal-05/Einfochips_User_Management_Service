package userservice.services.impls;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import userservice.dtos.UserRequestDTO;
import userservice.dtos.UserResponseDTO;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.services.UserCreateService;
import userservice.utility.MapToUserResponseDTO;

/**
 * Service implementation responsible for creating new users in the system.
 *
 * <p>
 * This service handles user creation workflow including:
 * <ul>
 * <li>Receiving user creation request data</li>
 * <li>Assigning a secure encoded default password</li>
 * <li>Setting user role and default status values</li>
 * <li>Persisting the user record to the database</li>
 * <li>Mapping entity data to response DTO</li>
 * </ul>
 *
 * <p>
 * Passwords are encoded using the configured
 * {@link org.springframework.security.crypto.password.PasswordEncoder} before
 * persistence.
 *
 * <p>
 * The default password is externally configured through application properties
 * using {@code app.default.password}.
 *
 * <p>
 * This service is transactional to ensure atomic database operations.
 *
 * @author Kinjal Mistry
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class UserCreateServiceImpl implements UserCreateService {

	/**
	 * Password encoder used for secure password hashing.
	 */
	private final PasswordEncoder passwordEncoder;

	/**
	 * Repository used for user persistence operations.
	 */
	private final UserRepository userRepository;

	/**
	 * Utility mapper for converting User entity to response DTO.
	 */
	private final MapToUserResponseDTO mapToUserResponseDTO;

	/**
	 * Default password assigned to newly created users.
	 *
	 * <p>
	 * Loaded from application configuration.
	 */
	@Value("${app.default.password}")
	private String defaultPassword;

	/**
	 * Creates a new user account using the provided request data.
	 *
	 * <p>
	 * Creation process:
	 * <ul>
	 * <li>Build new user entity</li>
	 * <li>Encode configured default password</li>
	 * <li>Assign requested role</li>
	 * <li>Mark account as active (not deleted)</li>
	 * <li>Persist user to database</li>
	 * <li>Convert saved entity to response DTO</li>
	 * </ul>
	 *
	 * @param request user creation request containing email and role
	 * @return created user response DTO
	 */
	@Override
	@Transactional
	public UserResponseDTO createUser(UserRequestDTO request) {
		User user = User.builder().email(request.email()).password(passwordEncoder.encode(defaultPassword))
				.role(request.role()).isDeleted(false).build();

		User savedUser = userRepository.save(user);
		return mapToUserResponseDTO.mapToUserResponseDTO(savedUser);
	}
}