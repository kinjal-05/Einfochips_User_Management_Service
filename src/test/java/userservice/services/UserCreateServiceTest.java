package userservice.services;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import userservice.dtos.UserRequestDTO;
import userservice.dtos.UserResponseDTO;
import userservice.enums.Role;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.security.CustomUserDetails;
import userservice.security.JwtService;
import userservice.services.impls.UserCreateServiceImpl;
import userservice.utility.MapToUserResponseDTO;

/**
 * Comprehensive unit test suite for
 * {@link UserCreateServiceImpl#createUser(UserRequestDTO)}.
 *
 * <p>
 * This test class validates the behavior of the user creation workflow in
 * isolation using Mockito and JUnit 5. It ensures correctness, data integrity,
 * and security when creating new users in the system.
 * </p>
 *
 * <h3>Test Coverage</h3>
 * <ul>
 * <li><b>Happy Path:</b> Verifies successful user creation and correct mapping
 * to {@link UserResponseDTO}.</li>
 *
 * <li><b>Password Handling:</b>
 * <ul>
 * <li>Ensures default password is always encoded before persistence</li>
 * <li>Validates encoded password is stored (never plain text)</li>
 * <li>Confirms encoder is invoked exactly once</li>
 * </ul>
 * </li>
 *
 * <li><b>Data Integrity:</b>
 * <ul>
 * <li>Ensures {@code isDeleted} is set to false for new users</li>
 * <li>Validates correct email and role assignment</li>
 * <li>Confirms full object state passed to repository</li>
 * </ul>
 * </li>
 *
 * <li><b>Repository Interaction:</b>
 * <ul>
 * <li>Ensures {@code save()} is called exactly once</li>
 * <li>Verifies no unnecessary repository interactions</li>
 * <li>Captures and inspects persisted entity using {@link ArgumentCaptor}</li>
 * </ul>
 * </li>
 *
 * <li><b>DTO Mapping:</b> Ensures response is derived from the persisted entity
 * returned by the repository, not from the pre-save object.</li>
 *
 * <li><b>Parameterized Testing:</b>
 * <ul>
 * <li>Validates behavior across all {@link Role} enum values</li>
 * <li>Tests multiple valid email formats and edge cases</li>
 * </ul>
 * </li>
 *
 * <li><b>Execution Order Validation:</b> Confirms password encoding occurs
 * before database persistence.</li>
 *
 * <li><b>Exception Handling:</b>
 * <ul>
 * <li>Propagates exceptions from {@link PasswordEncoder}</li>
 * <li>Propagates exceptions from {@link UserRepository}</li>
 * <li>Handles {@link DataIntegrityViolationException} (e.g., duplicate
 * email)</li>
 * </ul>
 * </li>
 *
 * <li><b>Edge Cases:</b>
 * <ul>
 * <li>Null request handling</li>
 * <li>Ensures no repository interaction when encoding fails</li>
 * <li>Ensures no null entity is passed to repository</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <h3>Testing Strategy</h3>
 * <ul>
 * <li>Uses {@link MockitoExtension} for mock initialization</li>
 * <li>Follows Arrange-Act-Assert pattern</li>
 * <li>Reusable helper methods for setup and verification</li>
 * <li>Focuses on behavior verification and contract testing</li>
 * </ul>
 *
 * <h3>Key Design Considerations</h3>
 * <ul>
 * <li>Ensures strict isolation of business logic</li>
 * <li>Validates security best practices (password hashing)</li>
 * <li>Prevents regression in user creation workflow</li>
 * <li>Maintains high readability and maintainability</li>
 * </ul>
 *
 * <p>
 * This test suite is designed to meet production-grade quality standards and
 * ensure reliability of the user creation functionality.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - createUser()")
@ActiveProfiles("test")
class UserCreateServiceTest {
	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private AuthenticationManager authenticationManager;
	@Mock
	private JwtService jwtService;
	@Mock
	private Authentication authentication;
	@Mock
	private CustomUserDetails customUserDetails;
	@Mock
	private SecurityContext securityContext;
	@Mock
	private MapToUserResponseDTO mapToUserResponseDTO;
	// Service under test
	@InjectMocks
	private UserCreateServiceImpl createUserService;

	private static final String DEFAULT_PASSWORD = "Temp@12345";
	private static final String ENCODED_PASSWORD = "$2a$10$encodedHashHere";
	private static final String TEST_EMAIL = "kinjal@gmail.com";
	private static final long SAVED_USER_ID = 1;
	private static final Role TEST_ROLE = Role.ROLE_USER;

	// Test data
	private UserRequestDTO userRequest;
	private User savedUser;

	/**
	 * Initializes test data and default service configuration.
	 *
	 * Setup responsibilities: - Inject default password via reflection - Prepare
	 * user request and saved entity mock
	 */
	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(createUserService, "defaultPassword", "Temp@12345");
		userRequest = new UserRequestDTO(TEST_EMAIL, TEST_ROLE);
		savedUser = User.builder().id(SAVED_USER_ID).email(TEST_EMAIL).password(ENCODED_PASSWORD).role(TEST_ROLE)
				.isDeleted(false).build();
	}

	/**
	 * Helper method to execute createUser flow with stubbed dependencies.
	 *
	 * Responsibilities: - Stub password encoding - Stub repository save behavior -
	 * Stub DTO mapping
	 */
	private UserResponseDTO executeCreateUser(UserRequestDTO request, User userToReturn) {
		given(passwordEncoder.encode(DEFAULT_PASSWORD)).willReturn(ENCODED_PASSWORD);
		given(userRepository.save(any(User.class))).willReturn(userToReturn);
		given(mapToUserResponseDTO.mapToUserResponseDTO(any(User.class))).willAnswer(invocation -> {
			User user = invocation.getArgument(0);

			return new UserResponseDTO(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt(),
					user.getUpdatedAt(), user.getCreatedById(), user.getUpdatedById());
		});
		return createUserService.createUser(request);
	}

	/**
	 * Captures the persisted User entity from repository save call.
	 */
	private User captureSavedUser() {
		ArgumentCaptor<User> argumentCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(argumentCaptor.capture());
		return argumentCaptor.getValue();
	}

	/**
	 * Ensures service returns correct DTO after successful user creation.
	 */
	@Test
	@Order(1)
	@DisplayName("Should create User and return DTO")
	void createUser_returnUserResponseDTO() {
		UserResponseDTO responseDTO = executeCreateUser(userRequest, savedUser);
		assertThat(responseDTO).isNotNull();
		assertThat(responseDTO.id()).isEqualTo(SAVED_USER_ID);
		assertThat(responseDTO.email()).isEqualTo(TEST_EMAIL);
		assertThat(responseDTO.role()).isEqualTo(TEST_ROLE);
	}

	/**
	 * Ensures default password is always encoded exactly once.
	 *
	 * Security expectation: - Default password must never be stored in plaintext
	 */
	@Test
	@Order(2)
	@DisplayName("Should always encode hard password")
	void createUser_AlwaysEncodePassword() {
		executeCreateUser(userRequest, savedUser);
		verify(passwordEncoder, times(1)).encode(DEFAULT_PASSWORD);
		verifyNoMoreInteractions(passwordEncoder);
	}

	/**
	 * Ensures newly created users are never marked as deleted.
	 */
	@Test
	@Order(3)
	@DisplayName("Should save user with isDeleted false")
	void createUser_SetIsDeletedFalse() {
		executeCreateUser(userRequest, savedUser);
		User argumentCaptor = captureSavedUser();
		assertThat(argumentCaptor.isDeleted()).isFalse();
	}

	/**
	 * Ensures password stored in DB is always encoded and never plaintext.
	 */
	@Test
	@Order(4)
	@DisplayName("Should save user with encoded password")
	void createUser_SavedEncodedPassword() {
		executeCreateUser(userRequest, savedUser);
		User argumentCaptor = captureSavedUser();
		assertThat(argumentCaptor.getPassword()).isEqualTo(ENCODED_PASSWORD).isNotEqualTo(DEFAULT_PASSWORD);
	}

	/**
	 * Ensures email is persisted exactly as provided in request.
	 */
	@Test
	@Order(5)
	@DisplayName("Should save user with exact email from request")
	void createUser_SavedWithExactEmail() {
		executeCreateUser(userRequest, savedUser);
		User argumentCaptor = captureSavedUser();
		assertThat(argumentCaptor.getEmail()).isEqualTo(TEST_EMAIL);
	}

	/**
	 * Ensures role is persisted exactly as provided in request.
	 */
	@Test
	@Order(6)
	@DisplayName("Should save user with exact role")
	void createUser_SavedWithExactRole() {
		executeCreateUser(userRequest, savedUser);
		User argumentCaptor = captureSavedUser();
		assertThat(argumentCaptor.getRole()).isEqualTo(TEST_ROLE);
	}

	/**
	 * Ensures password encoder is invoked exactly once per user creation.
	 */
	@Test
	@Order(7)
	@DisplayName("Should always call password encoder once")
	void createUser_CallPasswordEncoderOnlyOnce() {
		executeCreateUser(userRequest, savedUser);
		verify(passwordEncoder, times(1)).encode(DEFAULT_PASSWORD);
	}

	/**
	 * Ensures repository save operation is invoked exactly once.
	 */
	@Test
	@Order(8)
	@DisplayName("Should call user repository.save() only once")
	void createUser_CallUserRepoOnlyOnce() {
		executeCreateUser(userRequest, savedUser);
		verify(userRepository, times(1)).save(any(User.class));
	}

	/**
	 * Ensures no additional repository methods are invoked beyond save().
	 *
	 * Contract: - Only save() is allowed for persistence
	 */
	@Test
	@Order(9)
	@DisplayName("Should not call any other repo method")
	void createUser_NotCallAnyOtherMethod() {
		executeCreateUser(userRequest, savedUser);
		verify(userRepository, times(1)).save(any(User.class));
		verifyNoMoreInteractions(userRepository);
	}

	/**
	 * Ensures service correctly handles all Role enum values.
	 *
	 * Coverage expectation: - Every enum constant must be supported without failure
	 */
	@Order(10)
	@DisplayName("Should work for every enum type role")
	@ParameterizedTest(name = "role={0}")
	@EnumSource(Role.class)
	void createUser_WorkForEveryEnumRole(Role role) {
		UserRequestDTO userRequestDTO = new UserRequestDTO(TEST_EMAIL, role);
		User savedUser = User.builder().id(SAVED_USER_ID).email(TEST_EMAIL).password(ENCODED_PASSWORD).role(role)
				.isDeleted(false).build();
		UserResponseDTO userResponseDTO = executeCreateUser(userRequestDTO, savedUser);
		assertThat(userResponseDTO.role()).isEqualTo(role);
		User argumentCaptor = captureSavedUser();
		assertThat(argumentCaptor.getRole()).isEqualTo(role);
	}

	/**
	 * Ensures service correctly handles diverse valid email formats.
	 *
	 * Coverage: - RFC-like formats - uppercase emails - numeric local parts -
	 * subdomain-based emails
	 */
	@Order(11)
	@DisplayName("Checking edge cases for email")
	@ParameterizedTest(name = "email=\"{0}\"")
	@ValueSource(strings = { "simple@example.com", "user+tag@sub.domain.io", "UPPERCASE@EXAMPLE.COM",
			"123numeric@domain.org", "dots.in.local@part.com" })
	void createUser_CheckingEMAILEdgeCases(String email) {
		UserRequestDTO userRequestDTO = new UserRequestDTO(email, TEST_ROLE);
		User savedUser = User.builder().id(SAVED_USER_ID).email(email).password(ENCODED_PASSWORD).role(TEST_ROLE)
				.isDeleted(false).build();
		UserResponseDTO userResponseDTO = executeCreateUser(userRequestDTO, savedUser);
		assertThat(userResponseDTO.email()).isEqualTo(email);
		User argumentCaptor = captureSavedUser();
		assertThat(argumentCaptor.getEmail()).isEqualTo(email);
	}

	/**
	 * Ensures persisted entity contains correct final state.
	 *
	 * Contract: - Email must match request - Password must be encoded - Role must
	 * be preserved - isDeleted must always be false
	 */
	@Test
	@Order(12)
	@DisplayName("Fully Object is pass to the repo")
	void createUser_FullObjectStateInRepo() {
		executeCreateUser(userRequest, savedUser);
		User user = captureSavedUser();
		assertThat(user.getEmail()).isEqualTo(TEST_EMAIL);
		assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
		assertThat(user.getRole()).isEqualTo(TEST_ROLE);
		assertThat(user.isDeleted()).isFalse();
	}

	/**
	 * Ensures repository never receives a null User entity.
	 */
	@Test
	@Order(13)
	@DisplayName("Should Never Pass Null User to the repo")
	void createUser_NeverPassNullToRepo() {
		executeCreateUser(userRequest, savedUser);
		User argumentCaptor = captureSavedUser();
		assertThat(argumentCaptor).isNotNull();
	}

	/**
	 * Ensures exceptions from repository layer are propagated correctly.
	 */
	@Test
	@Order(14)
	@DisplayName("Should propogate Exception thrown by user rsponse")
	void createUser_PropogateExceptionFromUserResponse() {
		given(passwordEncoder.encode(DEFAULT_PASSWORD)).willReturn(ENCODED_PASSWORD);
		given(userRepository.save(any(User.class))).willThrow(new RuntimeException("DB unavailiable"));
		assertThatThrownBy(() -> createUserService.createUser(userRequest)).isInstanceOf(RuntimeException.class)
				.hasMessageContaining("DB unavailiable");
	}

	/**
	 * Ensures password encoding failures are propagated and prevent execution.
	 */
	@Test
	@Order(15)
	@DisplayName("Should propogate exception thrown by password encoder")
	void createUser_PropogateExceptionFromPasswordEncoder() {
		given(passwordEncoder.encode(DEFAULT_PASSWORD)).willThrow(new RuntimeException("Encoding error"));
		assertThatThrownBy(() -> createUserService.createUser(userRequest)).isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Encoding error");
		verifyNoMoreInteractions(userRepository);
	}

	/**
	 * Ensures repository is never called if password encoding fails.
	 */
	@Test
	@Order(16)
	@DisplayName("Should never call repo when password encoding fail")
	void createUser_NeverCallRepoOnPasswordEncoderFail() {
		given(passwordEncoder.encode(DEFAULT_PASSWORD)).willThrow(new RuntimeException("Encoding failure"));
		assertThatThrownBy(() -> createUserService.createUser(userRequest)).isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Encoding failure");
		verify(userRepository, never()).save(any(User.class));
	}

	/**
	 * Ensures database constraint violations are properly propagated.
	 */
	@Test
	@Order(17)
	@DisplayName("Should propogate data integrity violation exceptio in duplicated email")
	void createUser_DataIntegrityExceptionOnDuplicateEmail() {
		given(passwordEncoder.encode(DEFAULT_PASSWORD)).willReturn(ENCODED_PASSWORD);
		given(userRepository.save(any(User.class)))
				.willThrow(new DataIntegrityViolationException("Duplicate entry for email"));
		assertThatThrownBy(() -> createUserService.createUser(userRequest))
				.isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("Duplicate entry for email");
	}

	/**
	 * Ensures service rejects null requests immediately.
	 */
	@Test
	@Order(18)
	@DisplayName("Should throw null pointer exception when request is null")
	void createUser_NullPointerException() {
		assertThatThrownBy(() -> createUserService.createUser(null)).isInstanceOf(NullPointerException.class);
	}

	/**
	 * Ensures correct execution order: encode → save.
	 */
	@Test
	@Order(19)
	@DisplayName("Should encode password first before saving it into DB")
	void createUser_EncodePassFirstBeforeSavingInDB() {
		executeCreateUser(userRequest, savedUser);
		var inOrder = inOrder(passwordEncoder, userRepository);
		inOrder.verify(passwordEncoder).encode(DEFAULT_PASSWORD);
		inOrder.verify(userRepository).save(any(User.class));
	}

	/**
	 * Ensures service returns DTO mapped from actual repository result.
	 *
	 * Contract: - Response must reflect persisted entity, not prebuilt object
	 */
	@Test
	@Order(20)
	@DisplayName("should return the entity returned by the repository, not the one built internally")
	void createUser_ReturnsDTOMappedFromRepositoryResult() {
		User dbEnrichedUser = User.builder().id(1).email(TEST_EMAIL).password(ENCODED_PASSWORD).role(TEST_ROLE)
				.isDeleted(false).build();
		UserResponseDTO result = executeCreateUser(userRequest, dbEnrichedUser);
		assertThat(result.id()).isEqualTo(1);
	}
}
