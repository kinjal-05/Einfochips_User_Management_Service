	package userservice.servicesImpl;
	import java.time.LocalDateTime;
	import lombok.extern.slf4j.Slf4j;
	import org.springframework.transaction.annotation.Isolation;
	import org.springframework.transaction.annotation.Transactional;
	import userservice.dtos.*;
	import userservice.security.CustomUserDetails;
	import userservice.security.JwtService;
	import org.springframework.data.domain.Page;
	import org.springframework.data.domain.Pageable;
	import org.springframework.data.jpa.domain.Specification;
	import org.springframework.security.authentication.AuthenticationManager;
	import org.springframework.security.authentication.BadCredentialsException;
	import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
	import org.springframework.security.core.Authentication;
	import org.springframework.security.core.context.SecurityContextHolder;
	import org.springframework.security.crypto.password.PasswordEncoder;
	import org.springframework.stereotype.Service;
	import userservice.config.UserSpecification;
	import userservice.exceptions.ResourceNotFoundException;
	import userservice.models.User;
	import userservice.repositories.UserRepository;
	import userservice.services.UserService;
	import lombok.RequiredArgsConstructor;

@Service
@Slf4j
@RequiredArgsConstructor
	public class UserServiceImpl implements UserService {

		private final UserRepository userRepository;
		private final PasswordEncoder passwordEncoder;
		private final AuthenticationManager authenticationManager;
		private final JwtService jwtService;

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
			return mapToUserResponseDTO(savedUser);
		}

	/**
	 * Authenticates user credentials and generates JWT token.
	 *
	 * Flow:
	 * - Delegates authentication to AuthenticationManager.
	 * - On success, extracts authenticated user details.
	 * - Generates JWT token for stateless authentication.
	 *
	 * Security:
	 * - Throws BadCredentialsException if authentication fails.
	 * - No transaction required (read + auth operation only).
	 */
		@Override
		public LoginResponseDTO login(LoginRequestDTO request) {
			Authentication authentication = authenticationManager.authenticate(
						new UsernamePasswordAuthenticationToken(
								request.email(),
								request.password()
						)
				);
			CustomUserDetails customUserDetails =
					(CustomUserDetails) authentication.getPrincipal();
			String token = jwtService.generateToken(customUserDetails);
			User user = customUserDetails.getUser();
			log.info("{}",user);
			return new LoginResponseDTO(
					user.getId(),
					user.getEmail(),
					user.getRole(),
					token,
					"Login Successful"
			);
		}

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
			return usersPage.map(this::mapToUserResponseDTO);
		}

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
				isolation = Isolation.READ_COMMITTED,
				timeout = 10
		)
		public UserResponseDTO updateUser(long id, UserUpdateRequestDTO request) {
			User user = getUserOrThrow(id);
			User updatedUser = userRepository.save(user);
			return mapToUserResponseDTO(updatedUser);
		}

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
			User user = getUserOrThrow(id);
			return mapToUserResponseDTO(user);
		}

	/**
	 * Performs soft delete on a user.
	 *
	 * Transactional Behavior:
	 * - Wrapped in transaction to ensure delete operation consistency.
	 *
	 * Notes:
	 * - Uses @SQLDelete (Hibernate) to mark user as deleted instead of physical deletion.
	 */
		@Override
		@Transactional
		public void softDeleteUser(long id) {
			User user = getUserOrThrow(id);
			userRepository.delete(user);
		}

	/**
	 * Changes the password of the currently authenticated user.
	 *
	 * Transactional Behavior:
	 * - Uses READ_COMMITTED isolation to avoid dirty reads.
	 * - Rolls back for any exception to maintain data consistency.
	 *
	 * Flow:
	 * - Fetch authenticated user from security context.
	 * - Validate old password.
	 * - Encode and update new password.
	 *
	 * Security:
	 * - Prevents password update if old password is incorrect.
	 */
		@Override
		@Transactional(rollbackFor = Exception.class,isolation = Isolation.READ_COMMITTED)
		public void changePassword(ChangePasswordRequestDTO request) {
			String loggedInEmail = getCurrentUserEmail();
			User user = userRepository.findByEmail(loggedInEmail)
					.orElseThrow(() -> new ResourceNotFoundException(
							"User not found with email: " + loggedInEmail));

			if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
				throw new BadCredentialsException("Old password is incorrect");
			}
			user.setPassword(passwordEncoder.encode(request.newPassword()));

			userRepository.save(user);
			log.info("Coming After Save in Repo");
		}

	/**
	 * Retrieves the currently authenticated user's email from SecurityContext.
	 *
	 * Security:
	 * - Ensures user is authenticated before accessing context.
	 * - Throws exception for anonymous or unauthenticated access.
	 */
		private String getCurrentUserEmail() {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth == null || !auth.isAuthenticated()
					|| auth.getName().equals("anonymousUser")) {
				throw new BadCredentialsException("User is not authenticated");
			}
			return auth.getName();
		}

	/**
	 * Maps User entity to UserResponseDTO.
	 *
	 * Purpose:
	 * - Separates persistence model from API response model.
	 */
		private UserResponseDTO mapToUserResponseDTO(User user) {
			return new UserResponseDTO(
					user.getId(),
					user.getEmail(),
					user.getRole(),
					user.getCreatedAt(),
					user.getUpdatedAt(),
					user.getCreatedById(),
					user.getUpdatedById()
			);
		}

	/**
	 * Fetches an active user or throws exception if not found.
	 *
	 * Notes:
	 * - Ensures soft-deleted users are not returned.
	 * - Centralized validation method to avoid duplication.
	 */
		private User getUserOrThrow(long id) {
			return userRepository.findActiveById(id)
					.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
		}
	}
