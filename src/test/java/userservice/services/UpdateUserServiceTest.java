package userservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import userservice.Utility.GetActiveUser;
import userservice.Utility.MapToUserResponseDTO;
import userservice.dtos.UserResponseDTO;
import userservice.dtos.UserUpdateRequestDTO;
import userservice.enums.Role;
import userservice.exceptions.ResourceNotFoundException;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.security.CustomUserDetails;
import userservice.security.JwtService;
import java.time.LocalDateTime;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.inOrder;

/**
 * Unit test suite for {@link UpdateUserServiceImpl#updateUser(long, UserUpdateRequestDTO)}.
 *
 * <p>Tests are organized into the following behavioral groups:
 * <ul>
 *   <li><strong>Happy path</strong> (orders 1–14, 21, 26–29): verifies correct return values,
 *       argument capture, dependency call counts, and execution order on a successful update.</li>
 *   <li><strong>Not-found / invalid IDs</strong> (orders 15–20): verifies that
 *       {@link ResourceNotFoundException} is thrown and {@code save()} is never called
 *       when the target user does not exist.</li>
 *   <li><strong>Exception propagation</strong> (orders 22–25): verifies that persistence-layer
 *       and infrastructure exceptions bubble up unchanged.</li>
 * </ul>
 *
 * <p>All Spring context dependencies that are irrelevant to the update flow
 * ({@code PasswordEncoder}, {@code AuthenticationManager}, {@code JwtService}) are declared as
 * mocks and explicitly asserted to have zero interactions in the happy-path scenario.
 *
 * @see UpdateUserServiceImpl
 * @see GetActiveUser
 * @see MapToUserResponseDTO
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - updateUser()")
@ActiveProfiles("test")
public class UpdateUserServiceTest {

	// -------------------------------------------------------------------------
	// Mocked dependencies
	// -------------------------------------------------------------------------

	@Mock private UserRepository       userRepository;
	@Mock private PasswordEncoder      passwordEncoder;
	@Mock private AuthenticationManager authenticationManager;
	@Mock private JwtService           jwtService;
	@Mock private Authentication       authentication;
	@Mock private CustomUserDetails    customUserDetails;
	@Mock private SecurityContext      securityContext;
	@Mock private MapToUserResponseDTO mapToUserResponseDTO;
	@Mock private GetActiveUser        getActiveUser;

	/** The service under test, constructed by Mockito with the mocks above injected. */
	@InjectMocks
	private UpdateUserServiceImpl updateUserService;

	// -------------------------------------------------------------------------
	// Shared test constants
	// -------------------------------------------------------------------------

	private static final long          USER_ID        = 1L;
	private static final String        ORIGINAL_EMAIL = "original@example.com";
	private static final String        UPDATED_EMAIL  = "updated@example.com";
	private static final Role          ORIGINAL_ROLE  = Role.ROLE_USER;
	private static final Role          UPDATED_ROLE   = Role.ROLE_ADMIN;
	private static final LocalDateTime CREATED_AT     = LocalDateTime.of(2024, 1, 1, 10, 0);
	private static final LocalDateTime UPDATED_AT     = LocalDateTime.of(2024, 6, 1, 10, 0);

	// -------------------------------------------------------------------------
	// Per-test fixtures
	// -------------------------------------------------------------------------

	/** The user entity as it exists in the database before the update is applied. */
	private User existingUser;

	/** The user entity returned by {@code userRepository.save()}, simulating DB-enriched output. */
	private User savedUser;

	/** A fully populated update request carrying the new email and role. */
	private UserUpdateRequestDTO validRequest;

	// -------------------------------------------------------------------------
	// Setup
	// -------------------------------------------------------------------------

	/**
	 * Initialises the three test fixtures before every test method.
	 * {@code existingUser} represents the pre-update state, {@code savedUser} the post-save
	 * state returned by the repository, and {@code validRequest} the inbound DTO.
	 */
	@BeforeEach
	void setUp() {
		existingUser = User.builder()
				.id(USER_ID)
				.email(ORIGINAL_EMAIL)
				.role(ORIGINAL_ROLE)
				.isDeleted(false)
				.createdAt(CREATED_AT)
				.updatedAt(CREATED_AT)
				.createdById(0L)
				.updatedById(0L)
				.build();

		savedUser = User.builder()
				.id(USER_ID)
				.email(UPDATED_EMAIL)
				.role(UPDATED_ROLE)
				.isDeleted(false)
				.createdAt(CREATED_AT)
				.updatedAt(UPDATED_AT)
				.createdById(0L)
				.updatedById(USER_ID)
				.build();

		validRequest = new UserUpdateRequestDTO(UPDATED_EMAIL, UPDATED_ROLE);
	}

	// -------------------------------------------------------------------------
	// Stub helpers
	// -------------------------------------------------------------------------

	/**
	 * Configures the three core collaborators for the happy path:
	 * <ol>
	 *   <li>{@link GetActiveUser#getUserOrThrow(long)} returns {@code existingUser}.</li>
	 *   <li>{@link UserRepository#save(Object)} returns {@code savedUser}.</li>
	 *   <li>{@link MapToUserResponseDTO#mapToUserResponseDTO(User)} dynamically builds a
	 *       {@link UserResponseDTO} from whichever {@link User} is passed in, allowing
	 *       argument-capture tests to work correctly.</li>
	 * </ol>
	 */
	private void stubFoundAndSaved() {
		given(getActiveUser.getUserOrThrow(USER_ID))
				.willReturn(existingUser);

		given(userRepository.save(any(User.class)))
				.willReturn(savedUser);

		given(mapToUserResponseDTO.mapToUserResponseDTO(any(User.class)))
				.willAnswer(invocation -> {
					User user = invocation.getArgument(0);
					return new UserResponseDTO(
							user.getId(),
							user.getEmail(),
							user.getRole(),
							user.getCreatedAt(),
							user.getUpdatedAt(),
							user.getCreatedById(),
							user.getUpdatedById()
					);
				});
	}

	/**
	 * Configures {@link GetActiveUser#getUserOrThrow(long)} to throw
	 * {@link ResourceNotFoundException} for the given {@code id}, simulating a missing user.
	 *
	 * @param id the user ID that should be treated as non-existent
	 */
	private void stubNotFound(long id) {
		given(getActiveUser.getUserOrThrow(id))
				.willThrow(new ResourceNotFoundException("User not found with id: " + id));
	}

	// =========================================================================
	// Happy-path — return value assertions
	// =========================================================================

	/**
	 * Verifies that a successful update never returns {@code null}.
	 */
	@Test
	@Order(1)
	@DisplayName("should return a non-null UserResponseDTO on success")
	void updateUser_HappyPath_ReturnsNonNullDTO() {
		stubFoundAndSaved();

		UserResponseDTO result = updateUserService.updateUser(USER_ID, validRequest);

		assertThat(result).isNotNull();
	}

	/**
	 * Verifies that the returned DTO carries the same ID that was passed to the service.
	 */
	@Test
	@Order(2)
	@DisplayName("should return DTO with the id from the saved entity")
	void updateUser_ReturnsCorrectId() {
		stubFoundAndSaved();

		UserResponseDTO result = updateUserService.updateUser(USER_ID, validRequest);

		assertThat(result.id()).isEqualTo(USER_ID);
	}

	/**
	 * Verifies that the returned DTO reflects the <em>updated</em> email from the saved entity,
	 * not the original email from the pre-update state.
	 */
	@Test
	@Order(3)
	@DisplayName("should return DTO with the email from the saved entity")
	void updateUser_ReturnsEmailFromSavedEntity() {
		stubFoundAndSaved();

		UserResponseDTO result = updateUserService.updateUser(USER_ID, validRequest);

		assertThat(result.email()).isEqualTo(UPDATED_EMAIL);
	}

	/**
	 * Verifies that the returned DTO carries the role from the post-save entity.
	 */
	@Test
	@Order(4)
	@DisplayName("should return DTO with the role from the saved entity")
	void updateUser_ReturnsRoleFromSavedEntity() {
		stubFoundAndSaved();

		UserResponseDTO result = updateUserService.updateUser(USER_ID, validRequest);

		assertThat(result.role()).isEqualTo(UPDATED_ROLE);
	}

	/**
	 * Verifies that {@code createdAt} is preserved unchanged through the update operation.
	 */
	@Test
	@Order(5)
	@DisplayName("should return DTO with createdAt from the saved entity")
	void updateUser_ReturnsCreatedAt() {
		stubFoundAndSaved();

		UserResponseDTO result = updateUserService.updateUser(USER_ID, validRequest);

		assertThat(result.createdAt()).isEqualTo(CREATED_AT);
	}

	/**
	 * Verifies that {@code updatedAt} in the returned DTO matches the timestamp set by the
	 * persistence layer on the saved entity.
	 */
	@Test
	@Order(6)
	@DisplayName("should return DTO with updatedAt from the saved entity")
	void updateUser_ReturnsUpdatedAt() {
		stubFoundAndSaved();

		UserResponseDTO result = updateUserService.updateUser(USER_ID, validRequest);

		assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
	}

	// =========================================================================
	// Happy-path — dependency call-count assertions
	// =========================================================================

	/**
	 * Verifies that {@link GetActiveUser#getUserOrThrow(long)} is invoked exactly once,
	 * confirming the service does not perform redundant lookups.
	 */
	@Test
	@Order(7)
	@DisplayName("should call getActiveUser exactly once")
	void updateUser_CallsGetActiveUserExactlyOnce() {
		stubFoundAndSaved();

		updateUserService.updateUser(USER_ID, validRequest);

		verify(getActiveUser, times(1)).getUserOrThrow(USER_ID);
	}

	/**
	 * Verifies that {@link UserRepository#save(Object)} is invoked exactly once,
	 * preventing inadvertent double-writes.
	 */
	@Test
	@Order(8)
	@DisplayName("should call userRepository.save() exactly once")
	void updateUser_CallsSaveExactlyOnce() {
		stubFoundAndSaved();

		updateUserService.updateUser(USER_ID, validRequest);

		verify(userRepository, times(1)).save(any(User.class));
	}

	/**
	 * Verifies the complete set of expected collaborator interactions and asserts that no
	 * additional (unexpected) interactions occur on any of the three core dependencies.
	 * This guards against accidental extra calls that could indicate logic drift.
	 */
	@Test
	@Order(9)
	@DisplayName("should invoke no extra dependencies beyond getUserOrThrow, save and mapper")
	void updateUser_NoExtraRepositoryInteractions() {
		stubFoundAndSaved();

		updateUserService.updateUser(USER_ID, validRequest);

		verify(getActiveUser, times(1)).getUserOrThrow(USER_ID);
		verify(userRepository, times(1)).save(any(User.class));
		verify(mapToUserResponseDTO, times(1)).mapToUserResponseDTO(any(User.class));

		verifyNoMoreInteractions(getActiveUser, userRepository, mapToUserResponseDTO);
	}

	/**
	 * Verifies that the update flow never touches security or authentication infrastructure.
	 * These dependencies are wired via {@code @InjectMocks} but must remain completely idle
	 * during a plain update operation.
	 */
	@Test
	@Order(10)
	@DisplayName("should never interact with passwordEncoder, authenticationManager or jwtService")
	void updateUser_NoInteractionsWithOtherDependencies() {
		stubFoundAndSaved();

		updateUserService.updateUser(USER_ID, validRequest);

		verifyNoInteractions(passwordEncoder);
		verifyNoInteractions(authenticationManager);
		verifyNoInteractions(jwtService);
	}

	// =========================================================================
	// Happy-path — argument capture assertions
	// =========================================================================

	/**
	 * Verifies that the object passed to {@code save()} is never {@code null}.
	 */
	@Test
	@Order(11)
	@DisplayName("should pass a non-null User to save()")
	void updateUser_SaveReceivesNonNullUser() {
		stubFoundAndSaved();

		updateUserService.updateUser(USER_ID, validRequest);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		assertThat(captor.getValue()).isNotNull();
	}

	/**
	 * Verifies that the entity forwarded to {@code save()} retains the original user ID,
	 * ensuring the service does not accidentally create a new entity.
	 */
	@Test
	@Order(12)
	@DisplayName("should pass user with the correct id to save()")
	void updateUser_SavedUserHasCorrectId() {
		stubFoundAndSaved();

		updateUserService.updateUser(USER_ID, validRequest);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(USER_ID);
	}

	/**
	 * Verifies that the soft-delete flag is {@code false} on the entity passed to {@code save()}.
	 * An update operation must never inadvertently mark a user as deleted.
	 */
	@Test
	@Order(13)
	@DisplayName("should pass user with isDeleted = false to save()")
	void updateUser_SavedUserIsNotDeleted() {
		stubFoundAndSaved();

		updateUserService.updateUser(USER_ID, validRequest);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		assertThat(captor.getValue().isDeleted()).isFalse();
	}

	/**
	 * Verifies that the service mutates the fetched entity in place rather than constructing a
	 * new one. The entity passed to {@code save()} must be the same instance (by ID) returned by
	 * {@link GetActiveUser#getUserOrThrow(long)}.
	 */
	@Test
	@Order(14)
	@DisplayName("should pass the same entity returned by findActiveById to save()")
	void updateUser_PassesFetchedEntityToSave() {
		stubFoundAndSaved();

		updateUserService.updateUser(USER_ID, validRequest);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(existingUser.getId());
	}

	// =========================================================================
	// Not-found / invalid ID scenarios
	// =========================================================================

	/**
	 * Verifies that a {@link ResourceNotFoundException} is thrown when no active user
	 * exists for the given ID.
	 */
	@Test
	@Order(15)
	@DisplayName("should throw ResourceNotFoundException when user does not exist")
	void updateUser_UserNotFound_ThrowsResourceNotFoundException() {
		stubNotFound(USER_ID);

		assertThatThrownBy(() -> updateUserService.updateUser(USER_ID, validRequest))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	/**
	 * Verifies that the exception message contains the requested ID so callers can build
	 * meaningful error responses without additional lookups.
	 */
	@Test
	@Order(16)
	@DisplayName("should include the id in the exception message")
	void updateUser_UserNotFound_ExceptionMessageContainsId() {
		stubNotFound(USER_ID);

		assertThatThrownBy(() -> updateUserService.updateUser(USER_ID, validRequest))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(String.valueOf(USER_ID));
	}

	/**
	 * Verifies that {@code save()} is never called when the user lookup fails early.
	 * This prevents partial writes to the database.
	 */
	@Test
	@Order(17)
	@DisplayName("should never call save() when user is not found")
	void updateUser_UserNotFound_SaveNeverCalled() {
		stubNotFound(USER_ID);

		assertThatThrownBy(() -> updateUserService.updateUser(USER_ID, validRequest))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(userRepository, never()).save(any());
	}

	/**
	 * Parameterized test verifying that {@link ResourceNotFoundException} is thrown and its
	 * message contains the offending ID for a range of non-existent ID values.
	 *
	 * @param nonExistentId an ID that has no corresponding active user record
	 */
	@ParameterizedTest(name = "non-existent id = {0}")
	@Order(18)
	@ValueSource(longs = {99L, 999L, Long.MAX_VALUE})
	@DisplayName("should throw ResourceNotFoundException for any non-existent id")
	void updateUser_NonExistentIds_ThrowsResourceNotFoundException(long nonExistentId) {
		given(getActiveUser.getUserOrThrow(nonExistentId))
				.willThrow(new ResourceNotFoundException("User not found with id: " + nonExistentId));

		assertThatThrownBy(() -> updateUserService.updateUser(nonExistentId, validRequest))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(String.valueOf(nonExistentId));
	}

	/**
	 * Verifies behaviour when {@code id = 0}, which is outside the valid positive-ID domain
	 * and should never resolve to a real user.
	 */
	@Test
	@Order(19)
	@DisplayName("should throw ResourceNotFoundException when id = 0")
	void updateUser_ZeroId_ThrowsResourceNotFoundException() {
		given(getActiveUser.getUserOrThrow(0L))
				.willThrow(new ResourceNotFoundException("User not found with id: 0"));

		assertThatThrownBy(() -> updateUserService.updateUser(0L, validRequest))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("0");
	}

	/**
	 * Verifies behaviour for a negative ID, which is always invalid and must never match
	 * any stored user.
	 */
	@Test
	@Order(20)
	@DisplayName("should throw ResourceNotFoundException when id is negative")
	void updateUser_NegativeId_ThrowsResourceNotFoundException() {
		given(getActiveUser.getUserOrThrow(-1L))
				.willThrow(new ResourceNotFoundException("User not found with id: -1"));

		assertThatThrownBy(() -> updateUserService.updateUser(-1L, validRequest))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("-1");
	}

	/**
	 * Parameterized test that verifies the exact ID value is forwarded to
	 * {@link GetActiveUser#getUserOrThrow(long)} for several representative valid IDs,
	 * including the edge case of {@link Long#MAX_VALUE}.
	 *
	 * @param id the user ID to pass to the service under test
	 */
	@Order(21)
	@ParameterizedTest(name = "valid id = {0}")
	@ValueSource(longs = {1L, 50L, 100L, Long.MAX_VALUE})
	@DisplayName("should pass the exact id to GetActiveUser")
	void updateUser_PassesCorrectIdToGetActiveUser(long id) {
		User user = User.builder()
				.id(id).email(ORIGINAL_EMAIL).role(ORIGINAL_ROLE)
				.isDeleted(false).createdAt(CREATED_AT).updatedAt(CREATED_AT)
				.createdById(0L).updatedById(0L).build();

		User saved = User.builder()
				.id(id).email(UPDATED_EMAIL).role(UPDATED_ROLE)
				.isDeleted(false).createdAt(CREATED_AT).updatedAt(UPDATED_AT)
				.createdById(0L).updatedById(id).build();

		given(getActiveUser.getUserOrThrow(id)).willReturn(user);
		given(userRepository.save(any(User.class))).willReturn(saved);

		updateUserService.updateUser(id, validRequest);

		verify(getActiveUser).getUserOrThrow(id);
	}

	// =========================================================================
	// Exception propagation from the persistence layer
	// =========================================================================

	/**
	 * Verifies that a {@link DataIntegrityViolationException} thrown by {@code save()} —
	 * e.g. due to a duplicate e-mail constraint — propagates unmodified to the caller.
	 */
	@Test
	@Order(22)
	@DisplayName("should propagate DataIntegrityViolationException from save()")
	void updateUser_SaveThrowsDataIntegrity_Propagates() {
		given(getActiveUser.getUserOrThrow(USER_ID)).willReturn(existingUser);
		given(userRepository.save(any(User.class)))
				.willThrow(new DataIntegrityViolationException("Duplicate email"));

		assertThatThrownBy(() -> updateUserService.updateUser(USER_ID, validRequest))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("Duplicate email");
	}

	/**
	 * Verifies that an {@link OptimisticLockingFailureException} from {@code save()} —
	 * indicating a concurrent modification conflict — propagates unmodified to the caller.
	 */
	@Test
	@Order(23)
	@DisplayName("should propagate OptimisticLockingFailureException from save()")
	void updateUser_SaveThrowsOptimisticLocking_Propagates() {
		given(getActiveUser.getUserOrThrow(USER_ID)).willReturn(existingUser);
		given(userRepository.save(any(User.class)))
				.willThrow(new OptimisticLockingFailureException("Version conflict"));

		assertThatThrownBy(() -> updateUserService.updateUser(USER_ID, validRequest))
				.isInstanceOf(OptimisticLockingFailureException.class)
				.hasMessageContaining("Version conflict");
	}

	/**
	 * Verifies that any unexpected {@link RuntimeException} from {@code save()} — such as a
	 * transient database connectivity failure — propagates unmodified to the caller.
	 */
	@Test
	@Order(24)
	@DisplayName("should propagate RuntimeException from save()")
	void updateUser_SaveThrowsRuntimeException_Propagates() {
		given(getActiveUser.getUserOrThrow(USER_ID)).willReturn(existingUser);
		given(userRepository.save(any(User.class)))
				.willThrow(new RuntimeException("Unexpected DB error"));

		assertThatThrownBy(() -> updateUserService.updateUser(USER_ID, validRequest))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Unexpected DB error");
	}

	/**
	 * Verifies that when {@link GetActiveUser#getUserOrThrow(long)} itself throws a
	 * {@link RuntimeException}, the exception propagates and {@code save()} is never reached.
	 * This confirms the service has no try-catch that could silently swallow infrastructure errors.
	 */
	@Test
	@Order(25)
	@DisplayName("should propagate RuntimeException from GetActiveUser and never call save()")
	void updateUser_FindActiveByIdThrows_SaveNeverCalled() {
		given(getActiveUser.getUserOrThrow(USER_ID))
				.willThrow(new RuntimeException("DB connection lost"));

		assertThatThrownBy(() -> updateUserService.updateUser(USER_ID, validRequest))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("DB connection lost");

		verify(userRepository, never()).save(any());
	}

	// =========================================================================
	// DTO mapping correctness
	// =========================================================================

	/**
	 * Verifies that every field of the saved entity is present and correct in the returned DTO.
	 * This is a comprehensive field-by-field mapping assertion to catch partial mapping bugs.
	 */
	@Test
	@Order(26)
	@DisplayName("should map all fields of the saved entity to the DTO")
	void updateUser_MapsAllFieldsFromSavedEntity() {
		stubFoundAndSaved();

		UserResponseDTO result = updateUserService.updateUser(USER_ID, validRequest);

		assertThat(result.id()).isEqualTo(savedUser.getId());
		assertThat(result.email()).isEqualTo(savedUser.getEmail());
		assertThat(result.role()).isEqualTo(savedUser.getRole());
		assertThat(result.createdAt()).isEqualTo(savedUser.getCreatedAt());
		assertThat(result.updatedAt()).isEqualTo(savedUser.getUpdatedAt());
		assertThat(result.createdById()).isEqualTo(savedUser.getCreatedById());
		assertThat(result.updatedById()).isEqualTo(savedUser.getUpdatedById());
	}

	/**
	 * Verifies that the DTO is derived from the <em>post-save</em> entity (the value returned by
	 * {@code userRepository.save()}) and not the <em>pre-save</em> entity fetched by
	 * {@code getUserOrThrow()}. This distinction matters when the repository enriches the entity
	 * with server-generated values (e.g. audit timestamps, version fields).
	 */
	@Test
	@Order(27)
	@DisplayName("should return DTO from the post-save entity, not the pre-save entity")
	void updateUser_ReturnsDTOFromSavedEntity_NotFetchedEntity() {
		stubFoundAndSaved();

		UserResponseDTO result = updateUserService.updateUser(USER_ID, validRequest);

		assertThat(result.email())
				.as("DTO must come from save() result, not the entity fetched by getUserOrThrow")
				.isEqualTo(UPDATED_EMAIL)
				.isNotEqualTo(ORIGINAL_EMAIL);
	}

	/**
	 * Verifies that DB-enriched audit fields ({@code updatedAt}, {@code updatedById}) set by the
	 * persistence layer are correctly reflected in the returned DTO. The service must not use the
	 * pre-save snapshot for building the response.
	 */
	@Test
	@Order(28)
	@DisplayName("should reflect DB-enriched data (audit fields) from the saved entity")
	void updateUser_ReflectsDbEnrichedAuditFields() {
		User dbEnriched = User.builder()
				.id(USER_ID).email(UPDATED_EMAIL).role(UPDATED_ROLE)
				.isDeleted(false).createdAt(CREATED_AT).updatedAt(UPDATED_AT)
				.createdById(0L).updatedById(USER_ID).build();

		given(getActiveUser.getUserOrThrow(USER_ID)).willReturn(existingUser);
		given(userRepository.save(any(User.class))).willReturn(dbEnriched);
		given(mapToUserResponseDTO.mapToUserResponseDTO(dbEnriched))
				.willReturn(new UserResponseDTO(
						dbEnriched.getId(), dbEnriched.getEmail(), dbEnriched.getRole(),
						dbEnriched.getCreatedAt(), dbEnriched.getUpdatedAt(),
						dbEnriched.getCreatedById(), dbEnriched.getUpdatedById()
				));

		UserResponseDTO result = updateUserService.updateUser(USER_ID, validRequest);

		assertThat(result).isNotNull();
		assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
		assertThat(result.updatedById()).isEqualTo(USER_ID);
	}

	// =========================================================================
	// Execution order assertion
	// =========================================================================

	/**
	 * Verifies the strict execution order of the three core steps:
	 * <ol>
	 *   <li>Fetch the active user via {@link GetActiveUser#getUserOrThrow(long)}.</li>
	 *   <li>Persist the mutated entity via {@link UserRepository#save(Object)}.</li>
	 *   <li>Map the persisted entity to a DTO via {@link MapToUserResponseDTO#mapToUserResponseDTO(User)}.</li>
	 * </ol>
	 * Out-of-order execution (e.g. mapping before saving) would produce stale data.
	 */
	@Test
	@Order(29)
	@DisplayName("should fetch user BEFORE saving it")
	void updateUser_FetchesBeforeSaving() {
		stubFoundAndSaved();

		updateUserService.updateUser(USER_ID, validRequest);

		var inOrder = inOrder(getActiveUser, userRepository, mapToUserResponseDTO);

		inOrder.verify(getActiveUser).getUserOrThrow(USER_ID);
		inOrder.verify(userRepository).save(any(User.class));
		inOrder.verify(mapToUserResponseDTO).mapToUserResponseDTO(any(User.class));
	}
}