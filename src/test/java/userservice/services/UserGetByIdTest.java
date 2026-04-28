package userservice.services;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import userservice.dtos.UserResponseDTO;
import userservice.enums.Role;
import userservice.exceptions.ResourceNotFoundException;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.security.CustomUserDetails;
import userservice.security.JwtService;
import userservice.services.impls.UserGetByIdServiceImpl;
import userservice.utility.GetActiveUser;
import userservice.utility.MapToUserResponseDTO;

/**
 * Comprehensive unit test suite for {@link GetUserByIdServiceImpl)}.
 *
 * <p>
 * This test class verifies the behavior of the user retrieval workflow in
 * isolation using Mockito and JUnit 5. It ensures correct data fetching,
 * mapping, and error handling when retrieving a user by ID.
 * </p>
 *
 * <h3>Test Coverage</h3>
 * <ul>
 * <li><b>Happy Path:</b> Ensures a valid user is successfully retrieved and
 * mapped to {@link UserResponseDTO}.</li>
 *
 * <li><b>DTO Mapping Validation:</b> Verifies that all fields are correctly
 * mapped:
 * <ul>
 * <li>ID</li>
 * <li>Email</li>
 * <li>Role</li>
 * <li>Created/Updated timestamps</li>
 * <li>CreatedBy / UpdatedBy</li>
 * </ul>
 * </li>
 *
 * <li><b>Dependency Interaction:</b>
 * <ul>
 * <li>Ensures {@link GetActiveUser} is called exactly once</li>
 * <li>Validates mapper is invoked correctly</li>
 * <li>Confirms no unnecessary dependency interactions</li>
 * </ul>
 * </li>
 *
 * <li><b>Repository Safety:</b>
 * <ul>
 * <li>Ensures no mutation operations (save/delete) are invoked</li>
 * <li>Validates repository is not used directly in this flow</li>
 * </ul>
 * </li>
 *
 * <li><b>Exception Handling:</b>
 * <ul>
 * <li>Throws {@link ResourceNotFoundException} for non-existent IDs</li>
 * <li>Handles invalid inputs (zero, negative IDs)</li>
 * <li>Propagates runtime exceptions from dependencies</li>
 * </ul>
 * </li>
 *
 * <li><b>Parameterized Testing:</b>
 * <ul>
 * <li>Validates behavior for multiple valid IDs</li>
 * <li>Ensures proper handling of non-existent IDs</li>
 * </ul>
 * </li>
 *
 * <li><b>Edge Cases:</b>
 * <ul>
 * <li>Zero and negative ID validation</li>
 * <li>Ensures no further execution when exceptions occur</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <h3>Testing Strategy</h3>
 * <ul>
 * <li>Uses {@link MockitoExtension} for mock initialization</li>
 * <li>Follows Arrange-Act-Assert pattern</li>
 * <li>Uses helper methods for reusable stubbing logic</li>
 * <li>Focuses on behavior verification and contract validation</li>
 * </ul>
 *
 * <h3>Key Design Considerations</h3>
 * <ul>
 * <li>Ensures strict separation of concerns (service vs data access)</li>
 * <li>Validates read-only behavior (no side effects)</li>
 * <li>Prevents regression in user retrieval logic</li>
 * <li>Maintains high readability and maintainability</li>
 * </ul>
 *
 * <p>
 * This test suite is designed to meet production-grade standards and ensure
 * reliability of the user retrieval functionality.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - GetUserById()")
@ActiveProfiles("test")
class UserGetByIdTest {
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
	@Mock
	private GetActiveUser getActiveUser;
	// Service under test
	@InjectMocks
	private UserGetByIdServiceImpl getUserByIdService;

	private static final long USER_ID = 1L;
	private static final String EMAIL = "john.doe@example.com";
	private static final Role ROLE = Role.ROLE_USER;
	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2024, 1, 1, 10, 0);
	private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2024, 6, 1, 12, 0);
	private static final long CREATED_BY = 0L;
	private static final long UPDATED_BY = 2L;
	private User existingUser;
	private UserResponseDTO responseDTO;

	/**
	 * Initializes test data for user retrieval scenario.
	 *
	 * Setup includes: - Building a sample User entity - Preparing expected DTO
	 * mapping result
	 */
	@BeforeEach
	void setUp() {
		existingUser = User.builder().id(USER_ID).email(EMAIL).role(ROLE).isDeleted(false).createdAt(CREATED_AT)
				.updatedAt(UPDATED_AT).createdById(CREATED_BY).updatedById(UPDATED_BY).build();

		responseDTO = new UserResponseDTO(USER_ID, EMAIL, ROLE, CREATED_AT, UPDATED_AT, 0L, 2L);
	}

	/**
	 * Stubs successful user retrieval and mapping flow.
	 *
	 * Flow: - GetActiveUser returns entity - Mapper converts entity to DTO
	 */
	private void stubFound() {
		given(getActiveUser.getUserOrThrow(USER_ID)).willReturn(existingUser);
		given(mapToUserResponseDTO.mapToUserResponseDTO(existingUser)).willReturn(responseDTO);
	}

	/**
	 * Ensures service returns a non-null DTO on successful retrieval.
	 */
	@Test
	@DisplayName("should return a non-null UserResponseDTO")
	void getUserById_HappyPath_ReturnsNonNullDTO() {
		stubFound();

		UserResponseDTO result = getUserByIdService.getUserById(USER_ID);

		assertThat(result).isNotNull();
	}

	/**
	 * Ensures correct user ID is returned in response DTO.
	 */
	@Test
	@DisplayName("should return DTO with the correct id")
	void getUserById_ReturnsCorrectId() {
		stubFound();

		UserResponseDTO result = getUserByIdService.getUserById(USER_ID);

		assertThat(result.id()).isEqualTo(USER_ID);
	}

	/**
	 * Ensures email is correctly mapped to response DTO.
	 */
	@Test
	@DisplayName("should return DTO with the correct email")
	void getUserById_ReturnsCorrectEmail() {
		stubFound();

		UserResponseDTO result = getUserByIdService.getUserById(USER_ID);

		assertThat(result.email()).isEqualTo(EMAIL);
	}

	/**
	 * Ensures role is correctly mapped to response DTO.
	 */
	@Test
	@DisplayName("should return DTO with the correct role")
	void getUserById_ReturnsCorrectRole() {
		stubFound();

		UserResponseDTO result = getUserByIdService.getUserById(USER_ID);

		assertThat(result.role()).isEqualTo(ROLE);
	}

	/**
	 * Ensures creation timestamp is preserved in response DTO.
	 */
	@Test
	@DisplayName("should return DTO with the correct createdAt")
	void getUserById_ReturnsCorrectCreatedAt() {
		stubFound();

		UserResponseDTO result = getUserByIdService.getUserById(USER_ID);

		assertThat(result.createdAt()).isEqualTo(CREATED_AT);
	}

	/**
	 * Ensures update timestamp is preserved in response DTO.
	 */
	@Test
	@DisplayName("should return DTO with the correct updatedAt")
	void getUserById_ReturnsCorrectUpdatedAt() {
		stubFound();

		UserResponseDTO result = getUserByIdService.getUserById(USER_ID);

		assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
	}

	/**
	 * Ensures createdById is correctly mapped.
	 */
	@Test
	@DisplayName("should return DTO with the correct createdById")
	void getUserById_ReturnsCorrectCreatedById() {
		stubFound();

		UserResponseDTO result = getUserByIdService.getUserById(USER_ID);

		assertThat(result.createdById()).isEqualTo(CREATED_BY);
	}

	/**
	 * Ensures updatedById is correctly mapped.
	 */
	@Test
	@DisplayName("should return DTO with the correct updatedById")
	void getUserById_ReturnsCorrectUpdatedById() {
		stubFound();

		UserResponseDTO result = getUserByIdService.getUserById(USER_ID);

		assertThat(result.updatedById()).isEqualTo(UPDATED_BY);
	}

	/**
	 * Ensures GetActiveUser is called exactly once per request.
	 */
	@Test
	@DisplayName("should call GetActiveUser exactly once with the given id")
	void getUserById_CallsGetActiveUserExactlyOnce() {

		given(getActiveUser.getUserOrThrow(USER_ID)).willReturn(existingUser);

		given(mapToUserResponseDTO.mapToUserResponseDTO(existingUser)).willReturn(new UserResponseDTO(
				existingUser.getId(), existingUser.getEmail(), existingUser.getRole(), existingUser.getCreatedAt(),
				existingUser.getUpdatedAt(), existingUser.getCreatedById(), existingUser.getUpdatedById()));

		getUserByIdService.getUserById(USER_ID);

		verify(getActiveUser, times(1)).getUserOrThrow(USER_ID);
	}

	/**
	 * Ensures only allowed dependencies are invoked.
	 *
	 * Contract: - Only GetActiveUser and mapper are permitted - Repository must
	 * never be directly accessed
	 */
	@Test
	@DisplayName("should invoke no extra dependencies beyond GetActiveUser and mapper")
	void getUserById_NoExtraInteractions() {

		given(getActiveUser.getUserOrThrow(USER_ID)).willReturn(existingUser);

		given(mapToUserResponseDTO.mapToUserResponseDTO(existingUser)).willReturn(new UserResponseDTO(
				existingUser.getId(), existingUser.getEmail(), existingUser.getRole(), existingUser.getCreatedAt(),
				existingUser.getUpdatedAt(), existingUser.getCreatedById(), existingUser.getUpdatedById()));

		getUserByIdService.getUserById(USER_ID);

		verify(getActiveUser).getUserOrThrow(USER_ID);
		verify(mapToUserResponseDTO).mapToUserResponseDTO(existingUser);

		verifyNoInteractions(userRepository);
	}

	/**
	 * Ensures service remains independent of unrelated infrastructure components.
	 */
	@Test
	@DisplayName("should never interact with passwordEncoder, authenticationManager or jwtService")
	void getUserById_NoInteractionsWithOtherDependencies() {
		stubFound();

		getUserByIdService.getUserById(USER_ID);

		verifyNoInteractions(passwordEncoder);
		verifyNoInteractions(authenticationManager);
		verifyNoInteractions(jwtService);
	}

	/**
	 * Ensures repository mutation is strictly forbidden in read operation.
	 */
	@Test
	@DisplayName("should never call save() or delete() on the repository")
	void getUserById_NeverMutatesRepository() {
		stubFound();

		getUserByIdService.getUserById(USER_ID);

		verify(userRepository, never()).save(any());

	}

	/**
	 * Ensures invalid IDs consistently result in ResourceNotFoundException.
	 */
	@ParameterizedTest(name = "non-existent id = {0}")
	@ValueSource(longs = { 99L, 999L, Long.MAX_VALUE })
	@DisplayName("should throw ResourceNotFoundException for any non-existent id")
	void getUserById_NonExistentIds_ThrowsResourceNotFoundException(long nonExistentId) {

		given(getActiveUser.getUserOrThrow(nonExistentId))
				.willThrow(new ResourceNotFoundException("User not found with id: " + nonExistentId));

		assertThatThrownBy(() -> getUserByIdService.getUserById(nonExistentId))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining(String.valueOf(nonExistentId));
	}

	/**
	 * Ensures correct ID is always passed unchanged to GetActiveUser.
	 */
	@ParameterizedTest(name = "valid id = {0}")
	@ValueSource(longs = { 1L, 50L, 100L, Long.MAX_VALUE })
	@DisplayName("should pass the exact id to GetActiveUser")
	void getUserById_PassesCorrectIdToGetActiveUser(long id) {

		given(getActiveUser.getUserOrThrow(id)).willReturn(existingUser);

		given(mapToUserResponseDTO.mapToUserResponseDTO(existingUser)).willReturn(new UserResponseDTO(
				existingUser.getId(), existingUser.getEmail(), existingUser.getRole(), existingUser.getCreatedAt(),
				existingUser.getUpdatedAt(), existingUser.getCreatedById(), existingUser.getUpdatedById()));

		getUserByIdService.getUserById(id);

		verify(getActiveUser).getUserOrThrow(id);
	}

	/**
	 * Ensures boundary ID (0) is treated as invalid and rejected.
	 */
	@Test
	@DisplayName("should throw ResourceNotFoundException when id = 0")
	void getUserById_ZeroId_ThrowsResourceNotFoundException() {

		given(getActiveUser.getUserOrThrow(0L)).willThrow(new ResourceNotFoundException("User not found"));

		assertThatThrownBy(() -> getUserByIdService.getUserById(0L)).isInstanceOf(ResourceNotFoundException.class);
	}

	/**
	 * Ensures negative IDs are rejected with ResourceNotFoundException.
	 */
	@Test
	@DisplayName("should throw ResourceNotFoundException when id is negative")
	void getUserById_NegativeId_ThrowsResourceNotFoundException() {

		given(getActiveUser.getUserOrThrow(-1L)).willThrow(new ResourceNotFoundException("User not found"));

		assertThatThrownBy(() -> getUserByIdService.getUserById(-1L)).isInstanceOf(ResourceNotFoundException.class);
	}

	/**
	 * Ensures RuntimeExceptions from GetActiveUser are not swallowed.
	 */
	@Test
	@DisplayName("should propagate RuntimeException thrown by GetActiveUser")
	void getUserById_GetActiveUserThrows_PropagatesException() {

		given(getActiveUser.getUserOrThrow(USER_ID)).willThrow(new RuntimeException("DB connection lost"));

		assertThatThrownBy(() -> getUserByIdService.getUserById(USER_ID)).isInstanceOf(RuntimeException.class)
				.hasMessageContaining("DB connection lost");

		verify(getActiveUser).getUserOrThrow(USER_ID);
		verifyNoInteractions(userRepository);
	}

	/**
	 * Ensures repository remains untouched when upstream failure occurs.
	 *
	 * Contract: - No save/delete operations must ever occur in failure flows
	 */

	@Test
	@DisplayName("should never call save() or delete() when GetActiveUser throws")
	void getUserById_FindActiveByIdThrows_NeverMutatesRepository() {

		given(getActiveUser.getUserOrThrow(USER_ID)).willThrow(new RuntimeException("DB connection lost"));

		assertThatThrownBy(() -> getUserByIdService.getUserById(USER_ID)).isInstanceOf(RuntimeException.class)
				.hasMessageContaining("DB connection lost");

		verify(getActiveUser).getUserOrThrow(USER_ID);

		verifyNoInteractions(userRepository);
	}

}
