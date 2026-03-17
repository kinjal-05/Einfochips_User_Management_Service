
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.UserRequestDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.dtos.UserResponseDTO;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.enums.Role;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.models.User;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.repositories.UserRepository;
import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.servicesImpl.UserServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UserService - registerUser() Tests")
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserServiceImpl userService;

	private static final String DEFAULT_PASSWORD   = "Temp@12345";
	private static final String ENCODED_PASSWORD   = "encodedPassword";
	private static final String ADMIN_EMAIL        = "admin@example.com";
	private static final String NEW_USER_EMAIL     = "test@example.com";
	private static final Long   ADMIN_ID           = 1L;
	private static final Long   SAVED_USER_ID      = 2L;

	private UserRequestDTO request;
	private User adminUser;
	private User savedUser;

	@BeforeEach
	void setUp() {
		request = new UserRequestDTO(NEW_USER_EMAIL, Role.ROLE_USER);

		adminUser = buildUser(ADMIN_ID, ADMIN_EMAIL, Role.ROLE_ADMIN);
		savedUser = buildUser(SAVED_USER_ID, NEW_USER_EMAIL, Role.ROLE_USER);
		savedUser.setCreatedById(ADMIN_ID);
		savedUser.setUpdatedById(ADMIN_ID);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private User buildUser(Long id, String email, Role role) {
		User user = new User();
		user.setId(id);
		user.setEmail(email);
		user.setRole(role);
		user.setDeleted(false);
		return user;
	}

	private void mockAuthenticatedAdmin() {
		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(
						ADMIN_EMAIL,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
				);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private void mockPasswordEncoder() {
		when(passwordEncoder.encode(DEFAULT_PASSWORD)).thenReturn(ENCODED_PASSWORD);
	}

	@Test
	@Order(1)
	@DisplayName("Should register user and set createdBy when admin is authenticated")
	void registerUser_WhenAdminAuthenticated_ShouldSetCreatedByAndUpdatedBy() {

		mockAuthenticatedAdmin();
		mockPasswordEncoder();
		when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(adminUser));
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		UserResponseDTO response = userService.registerUser(request);

		assertNotNull(response, "Response should not be null");
		assertEquals(NEW_USER_EMAIL, response.email(), "Email should match");
		assertEquals(SAVED_USER_ID, response.id(), "User ID should match");

		verify(passwordEncoder, times(1)).encode(DEFAULT_PASSWORD);
		verify(userRepository, times(1)).findByEmail(ADMIN_EMAIL);
		verify(userRepository, times(1)).save(any(User.class));
		verifyNoMoreInteractions(userRepository, passwordEncoder);
	}

	@Test
	@Order(2)
	@DisplayName("Should register user without createdBy when no authentication present")
	void registerUser_WhenNotAuthenticated_ShouldNotSetCreatedBy() {

		mockPasswordEncoder();
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		UserResponseDTO response = userService.registerUser(request);

		assertNotNull(response, "Response should not be null");

		verify(userRepository, never()).findByEmail(anyString());
		verify(userRepository, times(1)).save(any(User.class));
		verifyNoMoreInteractions(userRepository);
	}

	@Test
	@Order(3)
	@DisplayName("Should still save user when authenticated admin not found in DB")
	void registerUser_WhenAdminNotFoundInDB_ShouldStillSaveUser() {

		mockAuthenticatedAdmin();
		mockPasswordEncoder();
		when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
		when(userRepository.save(any(User.class))).thenReturn(savedUser);
		UserResponseDTO response = userService.registerUser(request);
		assertNotNull(response, "Response should not be null");
		verify(userRepository, times(1)).findByEmail(ADMIN_EMAIL);
		verify(userRepository, times(1)).save(any(User.class));
	}

	@Test
	@Order(4)
	@DisplayName("Should always encode password with default value")
	void registerUser_ShouldAlwaysEncodeDefaultPassword() {

		mockPasswordEncoder();
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		userService.registerUser(request);
		verify(passwordEncoder, times(1)).encode(DEFAULT_PASSWORD);
		verify(passwordEncoder, never()).encode(argThat(pwd -> !pwd.equals(DEFAULT_PASSWORD)));
	}

	@Test
	@Order(5)
	@DisplayName("Should throw DataIntegrityViolationException on duplicate email")
	void registerUser_WhenDuplicateEmail_ShouldThrowDataIntegrityViolationException() {

		mockPasswordEncoder();
		when(userRepository.save(any(User.class)))
				.thenThrow(new DataIntegrityViolationException("Duplicate entry for email"));

		DataIntegrityViolationException exception = assertThrows(
				DataIntegrityViolationException.class,
				() -> userService.registerUser(request),
				"Should throw DataIntegrityViolationException for duplicate email"
		);

		assertTrue(exception.getMessage().contains("Duplicate entry"),
				"Exception message should mention duplicate entry");

		verify(userRepository, times(1)).save(any(User.class));
	}

	@Test
	@Order(6)
	@DisplayName("Should set isDeleted to false on new user registration")
	void registerUser_ShouldSetIsDeletedFalse() {

		mockPasswordEncoder();

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		when(userRepository.save(userCaptor.capture())).thenReturn(savedUser);

		userService.registerUser(request);

		User capturedUser = userCaptor.getValue();
		assertFalse(capturedUser.isDeleted(), "isDeleted should be false for new user");
		assertEquals(NEW_USER_EMAIL, capturedUser.getEmail(), "Email should be set correctly");
		assertEquals(Role.ROLE_USER, capturedUser.getRole(), "Role should be set correctly");
	}

	@Test
	@Order(7)
	@DisplayName("Should correctly map saved user to UserResponseDTO")
	void registerUser_ShouldCorrectlyMapToResponseDTO() {

		mockPasswordEncoder();
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		UserResponseDTO response = userService.registerUser(request);

		assertAll("Response DTO field validation",
				() -> assertNotNull(response),
				() -> assertEquals(SAVED_USER_ID, response.id()),
				() -> assertEquals(NEW_USER_EMAIL, response.email()),
				() -> assertEquals(Role.ROLE_USER, response.role())
		);
	}
}