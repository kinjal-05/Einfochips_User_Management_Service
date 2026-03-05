package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.servicesImpl;

import java.time.LocalDateTime;

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

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.config.UserSpecification;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.DeleteResponseDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.LoginRequestDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.LoginResponseDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.SimpleUserDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.UserRequestDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.UserResponseDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.UserUpdateRequestDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.enums.Role;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.exceptions.ResourceAlreadyExistsException;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.exceptions.ResourceNotFoundException;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.models.User;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.repositories.UserRepository;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.services.UserService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;

	@Override
	public UserResponseDTO registerUser(UserRequestDTO request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new ResourceAlreadyExistsException("Email Already Registered");
		}

		User user = new User();
		user.setEmail(request.getEmail());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setRole(Role.ROLE_USER);
		user.setDeleted(false);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication != null && authentication.isAuthenticated()) {
			String LoggedInEmail = authentication.getName();
			userRepository.findByEmail(LoggedInEmail).ifPresent(adminUser -> {
				user.setCreatedBy(adminUser);
				user.setUpdatedBy(adminUser);
			});
		}
		User savedUser = userRepository.save(user);

		return UserResponseDTO.builder().id(savedUser.getId()).email(savedUser.getEmail()).role(savedUser.getRole())
				.createdAt(savedUser.getCreatedAt()).updatedAt(savedUser.getUpdatedAt())
				.createdBy(savedUser.getCreatedBy() != null ? SimpleUserDTO.fromEntity(savedUser.getCreatedBy()) : null)
				.updatedBy(savedUser.getUpdatedBy() != null ? SimpleUserDTO.fromEntity(savedUser.getUpdatedBy()) : null)
				.build();
	}

	@Override
	public LoginResponseDTO login(LoginRequestDTO request) {

		try {
			Authentication authentication = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
			User user = userRepository.findByEmail(request.getEmail())
					.orElseThrow(() -> new BadCredentialsException("Invalid Credentials"));
			return LoginResponseDTO.builder().userId(user.getId()).email(user.getEmail()).role(user.getRole())
					.message("Login Successful").build();
		} catch (Exception e) {
			System.out.println("Authentication failed: " + e.getMessage());
			throw e;
		}
	}

	@Override
	public Page<UserResponseDTO> searchUsers(String email, Role role, String createdBy, String updatedBy,
			LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {

		Specification<User> specification = UserSpecification.filterUsers(email, role, createdBy, updatedBy, fromDate,
				toDate);

		Page<User> usersPage = userRepository.findAll(specification, pageable);

		if (usersPage.isEmpty()) {
			throw new ResourceNotFoundException("No users found with given filters");
		}

		return usersPage.map(user -> UserResponseDTO.builder().id(user.getId()).email(user.getEmail())
				.role(user.getRole()).createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt())
				.createdBy(user.getCreatedBy() != null ? SimpleUserDTO.fromEntity(user.getCreatedBy()) : null)
				.updatedBy(user.getUpdatedBy() != null ? SimpleUserDTO.fromEntity(user.getUpdatedBy()) : null).build());
	}

	@Override
	public UserResponseDTO updateUser(Long id, UserUpdateRequestDTO request) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
		if (request.getEmail() != null) {
			user.setEmail(request.getEmail());
		}

		if (request.getRole() != null) {
			user.setRole(request.getRole());
		}

		if (request.getIsDeleted() != null) {
			user.setDeleted(request.getIsDeleted());
		}
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated()) {
			String loggedInEmail = authentication.getName();
			userRepository.findByEmail(loggedInEmail).ifPresent(adminUser -> user.setUpdatedBy(adminUser));
		}

		User updatedUser = userRepository.save(user);
		return UserResponseDTO.builder().id(updatedUser.getId()).email(updatedUser.getEmail())
				.role(updatedUser.getRole()).createdAt(updatedUser.getCreatedAt()).updatedAt(updatedUser.getUpdatedAt())
				.createdBy(updatedUser.getCreatedBy() != null ? SimpleUserDTO.fromEntity(updatedUser.getCreatedBy())
						: null)
				.updatedBy(updatedUser.getUpdatedBy() != null ? SimpleUserDTO.fromEntity(updatedUser.getUpdatedBy())
						: null)
				.build();
	}

	@Override
	public UserResponseDTO getUserById(Long id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

		return UserResponseDTO.builder().id(user.getId()).email(user.getEmail()).role(user.getRole())
				.createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt())
				.createdBy(user.getCreatedBy() != null ? SimpleUserDTO.fromEntity(user.getCreatedBy()) : null)
				.updatedBy(user.getUpdatedBy() != null ? SimpleUserDTO.fromEntity(user.getUpdatedBy()) : null).build();
	}

	@Override
	public DeleteResponseDTO softDeleteUser(Long id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
		user.setDeleted(true);
		User deletedUser = userRepository.save(user);
		return DeleteResponseDTO.builder().id(deletedUser.getId()).email(deletedUser.getEmail())
				.role(deletedUser.getRole()).createdAt(deletedUser.getCreatedAt()).updatedAt(deletedUser.getUpdatedAt())
				.createdBy(deletedUser.getCreatedBy() != null ? SimpleUserDTO.fromEntity(deletedUser.getCreatedBy())
						: null)
				.updatedBy(deletedUser.getUpdatedBy() != null ? SimpleUserDTO.fromEntity(deletedUser.getUpdatedBy())
						: null)
				.isDeleted(deletedUser.isDeleted()).message("User soft deleted successfully").build();
	}
}
