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

		@Override
		@Transactional(readOnly = true)
		public UserResponseDTO getUserById(long id) {
			User user = getUserOrThrow(id);
			return mapToUserResponseDTO(user);
		}

		@Override
		@Transactional
		public void softDeleteUser(long id) {
			User user = getUserOrThrow(id);
			userRepository.delete(user);
		}

		@Override
		@Transactional(rollbackFor = Exception.class)
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

		private String getCurrentUserEmail() {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth == null || !auth.isAuthenticated()
					|| auth.getName().equals("anonymousUser")) {
				throw new BadCredentialsException("User is not authenticated");
			}
			return auth.getName();
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

		private User getUserOrThrow(long id) {
			return userRepository.findActiveById(id)
					.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
		}
	}
