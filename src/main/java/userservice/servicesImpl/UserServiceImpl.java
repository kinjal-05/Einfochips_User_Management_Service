	package userservice.servicesImpl;

	import java.time.LocalDateTime;

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
	@RequiredArgsConstructor
	public class UserServiceImpl implements UserService {
		private final UserRepository userRepository;
		private final PasswordEncoder passwordEncoder;
		private final AuthenticationManager authenticationManager;
		private final JwtService jwtService;

		@Override
		public UserResponseDTO registerUser(UserRequestDTO request) {

			String defaultPassword = "Temp@12345";

			User user = new User();
			user.setEmail(request.email());
			user.setPassword(passwordEncoder.encode(defaultPassword));
			user.setRole(request.role());
			user.setDeleted(false);

			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

			if (authentication != null && authentication.isAuthenticated()) {
				String loggedInEmail = authentication.getName();
				userRepository.findByEmail(loggedInEmail).ifPresent(adminUser -> {
					user.setCreatedById(adminUser.getId());
					user.setUpdatedById(adminUser.getId());
				});
			}


				User savedUser = userRepository.save(user);
				return mapToUserResponseDTO(savedUser);

		}


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

			return new LoginResponseDTO(
					user.getId(),
					user.getEmail(),
					user.getRole(),
					token,
					"Login Successful"
			);
		}

		@Override
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

			if (usersPage.isEmpty()) {
				throw new ResourceNotFoundException("No users found with given filters");
			}

			return usersPage.map(user -> new UserResponseDTO(
					user.getId(),
					user.getEmail(),
					user.getRole(),
					user.getCreatedAt(),
					user.getUpdatedAt(),
					user.getCreatedById(),
					user.getUpdatedById()
			));
		}

		@Override
		public UserResponseDTO updateUser(long id, UserUpdateRequestDTO request) {

			User user = userRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

			if (request.email() != null) {
				user.setEmail(request.email());
			}

			if (request.role() != null) {
				user.setRole(request.role());
			}

			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

			if (authentication != null && authentication.isAuthenticated()) {

				String loggedInEmail = authentication.getName();

				userRepository.findByEmail(loggedInEmail).ifPresent(adminUser -> {
					user.setUpdatedById(adminUser.getId());
				});
			}

			User updatedUser = userRepository.save(user);

			return new UserResponseDTO(
					updatedUser.getId(),
					updatedUser.getEmail(),
					updatedUser.getRole(),
					updatedUser.getCreatedAt(),
					updatedUser.getUpdatedAt(),
					updatedUser.getCreatedById(),
					updatedUser.getUpdatedById()
			);
		}

		@Override
		public UserResponseDTO getUserById(long id) {

			User user = userRepository.findById(id)
					.orElse(null);
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

		@Override
		public DeleteResponseDTO softDeleteUser(long id) {

			User user = userRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

			user.setDeleted(true);
			user.setDeletedTimestamp(LocalDateTime.now());

			User deletedUser = userRepository.save(user);

			return new DeleteResponseDTO(
					deletedUser.getId(),
					deletedUser.getEmail(),
					deletedUser.getRole(),
					deletedUser.getCreatedAt(),
					deletedUser.getUpdatedAt(),
					deletedUser.getCreatedById(),
					deletedUser.getUpdatedById(),
					deletedUser.isDeleted(),
					"User soft deleted successfully"
			);
		}

		@Override
		public ChangePasswordResponseDTO changePassword(ChangePasswordRequestDTO request) {

			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			String loggedInEmail = authentication.getName();

			User user = userRepository.findByEmail(loggedInEmail)
					.orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + loggedInEmail));

			if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
				throw new BadCredentialsException("Old password is incorrect");
			}

			user.setPassword(passwordEncoder.encode(request.newPassword()));

			user.setUpdatedById(user.getId());

			User updatedUser = userRepository.save(user);

			return new ChangePasswordResponseDTO(
					updatedUser.getId(),
					updatedUser.getEmail(),
					"Password changed successfully"
			);
		}

	}
