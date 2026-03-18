package userservice.services;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import userservice.dtos.*;
import userservice.enums.Role;
import userservice.exceptions.ResourceNotFoundException;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.security.CustomUserDetails;
import userservice.security.JwtService;
import userservice.servicesImpl.UserServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit 5 test suite for UserService.
 *
 * This test class validates:
 * - User registration
 * - Login with JWT token generation
 * - User update operations
 * - Soft deletion
 * - Password change
 * - Search with paging
 *
 * Uses Mockito for mocking dependencies.
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UserService - Complete Test Suite")
class UserServiceTest {

	// Mocked dependencies
	@Mock private UserRepository userRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private AuthenticationManager authenticationManager;
	@Mock private JwtService jwtService;

	// Service under test
	@InjectMocks private UserServiceImpl userService;

	// Constants used in tests
	private static final String DEFAULT_PASSWORD  = "Temp@12345";
	private static final String ENCODED_PASSWORD  = "encodedPassword";
	private static final String ADMIN_EMAIL       = "admin@example.com";
	private static final String USER_EMAIL        = "test@example.com";
	private static final String LOGIN_PASSWORD    = "password123";
	private static final String JWT_TOKEN         = "mocked-jwt-token";

	private static final Long ADMIN_ID = 1L;
	private static final Long USER_ID  = 2L;

	// Test data
	private UserRequestDTO userRequest;
	private User adminUser;
	private User savedUser;
	private LoginRequestDTO loginRequest;
	private CustomUserDetails customUserDetails;

	/**
	 * Initialize common objects before each test.
	 */
	@BeforeEach
	void setUp() {
		userRequest  = new UserRequestDTO(USER_EMAIL, Role.ROLE_USER);
		adminUser    = buildUser(ADMIN_ID, ADMIN_EMAIL, Role.ROLE_ADMIN);
		savedUser    = buildUser(USER_ID, USER_EMAIL, Role.ROLE_USER);

		// Set audit fields
		savedUser.setCreatedById(ADMIN_ID);
		savedUser.setUpdatedById(ADMIN_ID);

		loginRequest      = new LoginRequestDTO(USER_EMAIL, LOGIN_PASSWORD);
		customUserDetails = new CustomUserDetails(savedUser);
	}

	/**
	 * Clear security context after each test to prevent test bleed.
	 */
	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	/**
	 * Helper to build a User entity with id, email, and role.
	 */
	private User buildUser(Long id, String email, Role role) {
		User user = new User();
		user.setId(id);
		user.setEmail(email);
		user.setRole(role);
		user.setDeleted(false);
		return user;
	}

	/**
	 * Helper to mock an authenticated admin in security context.
	 */
	private void mockAdminAuth() {
		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(
						ADMIN_EMAIL,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
				);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	/**
	 * Helper to mock password encoding.
	 */
	private void mockPasswordEncoder() {
		when(passwordEncoder.encode(DEFAULT_PASSWORD)).thenReturn(ENCODED_PASSWORD);
	}

	/**
	 * Helper to build authentication object for a user.
	 */
	private Authentication buildAuthentication(CustomUserDetails details) {
		return new UsernamePasswordAuthenticationToken(
				details, null, details.getAuthorities()
		);
	}

	// =========================
	// ====== REGISTRATION =====
	// =========================

	@Test
	@Order(1)
	@DisplayName("Register - success with admin auth sets createdById")
	void registerUser_success() {
		mockAdminAuth();
		mockPasswordEncoder();

		when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(adminUser));
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		UserResponseDTO response = userService.registerUser(userRequest);

		assertNotNull(response);
		assertEquals(USER_EMAIL, response.email());
		assertEquals(USER_ID, response.id());

		verify(passwordEncoder).encode(DEFAULT_PASSWORD);
		verify(userRepository).findByEmail(ADMIN_EMAIL);
		verify(userRepository).save(any(User.class));
	}

	@Test
	@Order(2)
	@DisplayName("Register - no auth context skips createdById lookup")
	void registerUser_noAuth() {
		mockPasswordEncoder();
		when(userRepository.save(any())).thenReturn(savedUser);

		userService.registerUser(userRequest);

		verify(userRepository, never()).findByEmail(anyString());
		verify(userRepository).save(any());
	}

	@Test
	@Order(3)
	@DisplayName("Register - duplicate email throws DataIntegrityViolationException")
	void registerUser_duplicateEmail() {
		mockPasswordEncoder();
		when(userRepository.save(any()))
				.thenThrow(new DataIntegrityViolationException("Duplicate entry"));

		assertThrows(DataIntegrityViolationException.class,
				() -> userService.registerUser(userRequest));
	}

	@Test
	@Order(4)
	@DisplayName("Register - password is encoded before saving")
	void registerUser_shouldEncodePassword() {
		mockPasswordEncoder();
		when(userRepository.save(any())).thenReturn(savedUser);

		userService.registerUser(userRequest);

		verify(passwordEncoder).encode(DEFAULT_PASSWORD);
	}

	@Test
	@Order(5)
	@DisplayName("Register - response DTO fields are correctly mapped")
	void registerUser_mapping() {
		mockPasswordEncoder();
		when(userRepository.save(any())).thenReturn(savedUser);

		UserResponseDTO response = userService.registerUser(userRequest);

		assertAll(
				() -> assertNotNull(response),
				() -> assertEquals(USER_ID,        response.id()),
				() -> assertEquals(USER_EMAIL,     response.email()),
				() -> assertEquals(Role.ROLE_USER, response.role())
		);
	}

	// =========================
	// ======== LOGIN ==========
	// =========================

	@Test
	@Order(6)
	@DisplayName("Login - success returns JWT token and user details")
	void login_success() {
		Authentication auth = buildAuthentication(customUserDetails);

		when(authenticationManager.authenticate(any())).thenReturn(auth);
		when(jwtService.generateToken(customUserDetails)).thenReturn(JWT_TOKEN);

		LoginResponseDTO response = userService.login(loginRequest);

		assertNotNull(response);
		assertEquals(USER_EMAIL,         response.email());
		assertEquals(JWT_TOKEN,          response.token());
		assertEquals("Login Successful", response.message());

		verify(authenticationManager, times(1)).authenticate(any());
		verify(jwtService,            times(1)).generateToken(customUserDetails);
	}

	@Test
	@Order(7)
	@DisplayName("Login - bad credentials throws exception and skips token generation")
	void login_invalidCredentials() {
		when(authenticationManager.authenticate(any()))
				.thenThrow(new RuntimeException("Bad credentials"));

		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> userService.login(loginRequest));

		assertTrue(ex.getMessage().contains("Bad credentials"));
		verify(jwtService, never()).generateToken(any());
	}

	@Test
	@Order(8)
	@DisplayName("Login - jwtService.generateToken is called with correct details")
	void login_shouldCallJwtService() {
		Authentication auth = buildAuthentication(customUserDetails);

		when(authenticationManager.authenticate(any())).thenReturn(auth);
		when(jwtService.generateToken(customUserDetails)).thenReturn(JWT_TOKEN);

		userService.login(loginRequest);

		verify(jwtService).generateToken(customUserDetails);
	}

	@Test
	@Order(9)
	@DisplayName("Login - response contains correct id, email, role and token")
	void login_responseDataValidation() {
		Authentication auth = buildAuthentication(customUserDetails);

		when(authenticationManager.authenticate(any())).thenReturn(auth);
		when(jwtService.generateToken(customUserDetails)).thenReturn(JWT_TOKEN);

		LoginResponseDTO response = userService.login(loginRequest);

		assertAll("Response validation",
				() -> assertEquals(savedUser.getId(),    response.id()),
				() -> assertEquals(savedUser.getEmail(), response.email()),
				() -> assertEquals(savedUser.getRole(),  response.role()),
				() -> assertEquals(JWT_TOKEN,            response.token())
		);
	}

	// =========================
	// ======== UPDATE =========
	// =========================

	@Test
	@Order(10)
	@DisplayName("Update - success with admin auth updates email and role")
	void updateUser_success() {
		UserUpdateRequestDTO updateRequest =
				new UserUpdateRequestDTO("updated@example.com", Role.ROLE_ADMIN);

		mockAdminAuth();

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(savedUser));
		when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(adminUser));
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		UserResponseDTO response = userService.updateUser(USER_ID, updateRequest);

		assertNotNull(response);
		assertEquals("updated@example.com", response.email());
		assertEquals(Role.ROLE_ADMIN,       response.role());

		verify(userRepository).findById(USER_ID);
		verify(userRepository).findByEmail(ADMIN_EMAIL);
		verify(userRepository).save(any(User.class));
	}

	@Test
	@Order(11)
	@DisplayName("Update - user not found throws ResourceNotFoundException")
	void updateUser_notFound() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		UserUpdateRequestDTO updateRequest =
				new UserUpdateRequestDTO("test@test.com", Role.ROLE_USER);

		assertThrows(ResourceNotFoundException.class,
				() -> userService.updateUser(USER_ID, updateRequest));

		verify(userRepository).findById(USER_ID);
		verify(userRepository, never()).save(any());
	}

	@Test
	@Order(12)
	@DisplayName("Update - no auth context skips updatedById lookup")
	void updateUser_noAuth() {
		UserUpdateRequestDTO updateRequest =
				new UserUpdateRequestDTO("test@test.com", Role.ROLE_USER);

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(savedUser));
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		userService.updateUser(USER_ID, updateRequest);

		verify(userRepository, never()).findByEmail(anyString());
		verify(userRepository).save(any(User.class));
	}

	@Test
	@Order(13)
	@DisplayName("Update - null role preserves existing role")
	void updateUser_onlyEmail() {
		UserUpdateRequestDTO updateRequest =
				new UserUpdateRequestDTO("new@email.com", null);

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(savedUser));
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		UserResponseDTO response = userService.updateUser(USER_ID, updateRequest);

		assertEquals("new@email.com",    response.email());
		assertEquals(savedUser.getRole(), response.role());
	}

	// =========================
	// ======= SOFT DELETE =====
	// =========================

	@Test
	@Order(14)
	@DisplayName("SoftDelete - success marks user as deleted and saves")
	void softDeleteUser_success() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(savedUser));
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		userService.softDeleteUser(USER_ID);

		assertTrue(savedUser.isDeleted());
		verify(userRepository).findById(USER_ID);
		verify(userRepository).save(savedUser);
	}

	@Test
	@Order(15)
	@DisplayName("SoftDelete - user not found throws ResourceNotFoundException")
	void softDeleteUser_notFound() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> userService.softDeleteUser(USER_ID));

		verify(userRepository).findById(USER_ID);
		verify(userRepository, never()).save(any());
	}

	@Test
	@Order(16)
	@DisplayName("SoftDelete - saved entity has deleted=true via ArgumentCaptor")
	void softDeleteUser_shouldSetDeletedTrue() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(savedUser));
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		userService.softDeleteUser(USER_ID);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());

		assertTrue(captor.getValue().isDeleted());
	}

	// =========================
	// ===== CHANGE PASSWORD ===
	// =========================

	@Test
	@Order(17)
	@DisplayName("ChangePassword - success returns correct response DTO")
	void changePassword_success() {
		mockAdminAuth();

		ChangePasswordRequestDTO changeRequest =
				new ChangePasswordRequestDTO("oldPass", "newPass");

		savedUser.setPassword("encodedOldPass");

		when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(savedUser));
		when(passwordEncoder.matches("oldPass", "encodedOldPass")).thenReturn(true);
		when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPass");
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		ChangePasswordResponseDTO response = userService.changePassword(changeRequest);

		assertNotNull(response);
		assertEquals(USER_ID,                    response.userId());
		assertEquals(USER_EMAIL,                 response.email());
		assertEquals("Password changed successfully", response.message());

		verify(passwordEncoder).matches("oldPass", "encodedOldPass");
		verify(passwordEncoder).encode("newPass");
		verify(userRepository).save(any(User.class));
	}

	@Test
	@Order(18)
	@DisplayName("ChangePassword - user not found throws ResourceNotFoundException")
	void changePassword_userNotFound() {
		mockAdminAuth();

		ChangePasswordRequestDTO changeRequest =
				new ChangePasswordRequestDTO("oldPass", "newPass");

		when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> userService.changePassword(changeRequest));

		verify(userRepository).findByEmail(ADMIN_EMAIL);
		verify(userRepository, never()).save(any());
	}

	@Test
	@Order(19)
	@DisplayName("ChangePassword - wrong old password throws BadCredentialsException")
	void changePassword_wrongOldPassword() {
		mockAdminAuth();

		ChangePasswordRequestDTO changeRequest =
				new ChangePasswordRequestDTO("wrongOld", "newPass");

		savedUser.setPassword("encodedOldPass");

		when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(savedUser));
		when(passwordEncoder.matches("wrongOld", "encodedOldPass")).thenReturn(false);

		assertThrows(BadCredentialsException.class,
				() -> userService.changePassword(changeRequest));

		verify(passwordEncoder).matches("wrongOld", "encodedOldPass");
		verify(userRepository, never()).save(any());
	}

	@Test
	@Order(20)
	@DisplayName("ChangePassword - saved entity has new encoded password and correct updatedById")
	void changePassword_shouldUpdatePasswordAndAuditField() {
		mockAdminAuth();

		ChangePasswordRequestDTO changeRequest =
				new ChangePasswordRequestDTO("oldPass", "newPass");

		savedUser.setPassword("encodedOldPass");

		when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(savedUser));
		when(passwordEncoder.matches("oldPass", "encodedOldPass")).thenReturn(true);
		when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPass");
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		userService.changePassword(changeRequest);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());

		User captured = captor.getValue();
		assertEquals("encodedNewPass", captured.getPassword());
		assertEquals(savedUser.getId(), captured.getUpdatedById());
	}

	// =========================
	// ======== SEARCH =========
	// =========================

	@Test
	@Order(21)
	@DisplayName("Search - returns matching page of users")
	void searchUsers_success() {
		Pageable pageable = PageRequest.of(0, 10);

		UserSearchRequestDTO searchRequest =
				new UserSearchRequestDTO(USER_EMAIL, Role.ROLE_USER, ADMIN_ID, ADMIN_ID, null, null);

		when(userRepository.findAll(any(Specification.class), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(savedUser)));

		Page<UserResponseDTO> result = userService.searchUsers(searchRequest, pageable);

		assertNotNull(result);
		assertEquals(1, result.getContent().size());
		verify(userRepository).findAll(any(Specification.class), eq(pageable));
	}

	@Test
	@Order(22)
	@DisplayName("Search - empty result throws ResourceNotFoundException")
	void searchUsers_noData() {
		Pageable pageable = PageRequest.of(0, 10);

		UserSearchRequestDTO searchRequest =
				new UserSearchRequestDTO(null, null, 0L, 0L, null, null);

		when(userRepository.findAll(any(Specification.class), eq(pageable)))
				.thenReturn(Page.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> userService.searchUsers(searchRequest, pageable));
	}

	@Test
	@Order(23)
	@DisplayName("Search - response DTO fields are correctly mapped from User entity")
	void searchUsers_mapping() {
		Pageable pageable = PageRequest.of(0, 10);

		UserSearchRequestDTO searchRequest =
				new UserSearchRequestDTO(null, null, 0L, 0L, null, null);

		when(userRepository.findAll(any(Specification.class), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(savedUser)));

		Page<UserResponseDTO> result = userService.searchUsers(searchRequest, pageable);
		UserResponseDTO dto = result.getContent().get(0);

		assertAll(
				() -> assertEquals(savedUser.getId(),    dto.id()),
				() -> assertEquals(savedUser.getEmail(), dto.email()),
				() -> assertEquals(savedUser.getRole(),  dto.role())
		);
	}
}