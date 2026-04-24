package userservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import userservice.Utility.GetActiveUser;
import userservice.Utility.MapToUserResponseDTO;
import userservice.dtos.UserResponseDTO;
import userservice.enums.Role;
import userservice.exceptions.ResourceNotFoundException;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.security.CustomUserDetails;
import userservice.security.JwtService;
import java.time.LocalDateTime;
import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;


/**
 * Test suite for SoftDeleteUserServiceImpl.softDeleteUser()
 *
 * This suite validates the behavior of soft deletion in the User Service.
 *
 * Key responsibilities of the method under test:
 * 1. Fetch an active (non-deleted) user using GetActiveUser utility
 * 2. Delegate deletion to UserRepository.delete()
 * 3. Rely on @SQLDelete for converting DELETE into soft-delete UPDATE
 *
 * Important Design Contracts:
 * - This method is VOID → no return value should be asserted
 * - Soft delete is handled at the persistence layer (Hibernate @SQLDelete)
 * - Service MUST NOT manually modify isDeleted or deletedTimestamp
 * - Only active users should be fetched (via findActiveById internally)
 *
 * What is NOT tested here:
 * - Actual SQL execution (covered in integration tests)
 * - Hibernate behavior of @SQLDelete
 *
 * Testing Strategy:
 * - Behavior verification using Mockito
 * - Interaction-based assertions (verify calls & order)
 * - Exception propagation validation
 * - Argument capture to validate passed entity
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - softDeleteUser()")
@ActiveProfiles("test")
public class SoftDeleteUserTest {

	@Mock private UserRepository userRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private AuthenticationManager authenticationManager;
	@Mock private JwtService jwtService;
	@Mock private Authentication authentication;
	@Mock private CustomUserDetails customUserDetails;
	@Mock private SecurityContext securityContext;
	@Mock private MapToUserResponseDTO mapToUserResponseDTO;
	@Mock private GetActiveUser getActiveUser;

	@InjectMocks
	private SoftDeleteUserServiceImpl softDeleteUserService;

	private static final long          USER_ID    = 1L;
	private static final String        EMAIL      = "john.doe@example.com";
	private static final Role          ROLE       = Role.ROLE_USER;
	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2024, 1, 1, 10, 0);
	private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2024, 6, 1, 12, 0);

	private User existingUser;

	// ─── Fixture ──────────────────────────────────────────────────────────────

	@BeforeEach
	void setUp() {
		existingUser = User.builder()
				.id(USER_ID)
				.email(EMAIL)
				.password("encodedPassword")
				.role(ROLE)
				.isDeleted(false)
				.createdAt(CREATED_AT)
				.updatedAt(UPDATED_AT)
				.createdById(0L)
				.updatedById(0L)
				.build();
	}

	// ─── Shared stub helpers ──────────────────────────────────────────────────

	/**
	 * The service delegates user lookup to GetActiveUser.getUserOrThrow(),
	 * which internally calls userRepository.findActiveById().
	 * We stub the utility directly here — repository interactions are verified
	 * only where explicitly tested.
	 *
	 * NOTE: softDeleteUser() is void and never maps to a DTO, so
	 * mapToUserResponseDTO is NOT stubbed here.
	 *
	 * NOTE: @SQLDelete on the User entity means userRepository.delete()
	 * triggers an UPDATE (soft delete) at the DB level, not a real DELETE.
	 * At the unit-test level we just verify delete() is called with the
	 * correct entity — the SQL rewrite is a JPA/Hibernate concern.
	 */
	private void stubFound() {
		given(getActiveUser.getUserOrThrow(USER_ID)).willReturn(existingUser);
	}

	private void stubNotFound(long id) {
		given(getActiveUser.getUserOrThrow(id))
				.willThrow(new ResourceNotFoundException("User not found with id: " + id));
	}

	// ─── Happy path ───────────────────────────────────────────────────────────

	@Test
	@DisplayName("should soft delete user and complete without exception")
	void softDeleteUser_HappyPath_UpdatesUserSuccessfully() {
		stubFound();

		assertThatNoException().isThrownBy(
				() -> softDeleteUserService.softDeleteUser(USER_ID)
		);

		verify(userRepository).delete(any(User.class));
	}

	@Test
	@DisplayName("should return void — method has no return value")
	void softDeleteUser_ReturnsVoid() {
		stubFound();

		// Just asserting the call completes — void methods have no return to check
		softDeleteUserService.softDeleteUser(USER_ID);

		// Reaching here means the method returned normally
		assertThat(true).isTrue();
	}

	// ─── Repository interaction counts ───────────────────────────────────────

	@Test
	@DisplayName("should call getUserOrThrow() exactly once with the given id")
	void softDeleteUser_CallsGetUserOrThrowExactlyOnce() {
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		verify(getActiveUser, times(1)).getUserOrThrow(USER_ID);
	}

	@Test
	@DisplayName("should call delete() exactly once")
	void softDeleteUser_CallsDeleteExactlyOnce() {
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		verify(userRepository, times(1)).delete(any(User.class));
	}

	@Test
	@DisplayName("should invoke no extra repository methods beyond delete()")
	void softDeleteUser_NoExtraRepositoryInteractions() {
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		verify(userRepository, times(1)).delete(any(User.class));
		verifyNoMoreInteractions(userRepository);
	}

	@Test
	@DisplayName("should never call save() on the repository")
	void softDeleteUser_NeverCallsSave() {
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("should never call findById() — only findActiveById() via getUserOrThrow()")
	void softDeleteUser_NeverCallsFindById() {
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		verify(userRepository, never()).findById(any());
	}

	// ─── No interactions with unrelated dependencies ──────────────────────────

	@Test
	@DisplayName("should never interact with passwordEncoder, authenticationManager or jwtService")
	void softDeleteUser_NoInteractionsWithOtherDependencies() {
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		verifyNoInteractions(passwordEncoder);
		verifyNoInteractions(authenticationManager);
		verifyNoInteractions(jwtService);
	}

	@Test
	@DisplayName("should never interact with mapToUserResponseDTO — method is void")
	void softDeleteUser_NoInteractionWithMapper() {
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		verifyNoInteractions(mapToUserResponseDTO);
	}

	// ─── ArgumentCaptor assertions ────────────────────────────────────────────

	@Test
	@DisplayName("should pass a non-null User to delete()")
	void softDeleteUser_DeleteReceivesNonNullUser() {
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).delete(captor.capture());
		assertThat(captor.getValue()).isNotNull();
	}

	@Test
	@DisplayName("should pass the exact entity returned by getUserOrThrow to delete()")
	void softDeleteUser_PassesFetchedEntityToDelete() {
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).delete(captor.capture());

		assertThat(captor.getValue().getId()).isEqualTo(USER_ID);
		assertThat(captor.getValue().getEmail()).isEqualTo(EMAIL);
	}

	@Test
	@DisplayName("should pass entity with correct id to delete()")
	void softDeleteUser_DeletedEntityHasCorrectId() {
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).delete(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(USER_ID);
	}

	@Test
	@DisplayName("should pass entity with correct email to delete()")
	void softDeleteUser_DeletedEntityHasCorrectEmail() {
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).delete(captor.capture());
		assertThat(captor.getValue().getEmail()).isEqualTo(EMAIL);
	}

	@Test
	@DisplayName("should pass entity with correct role to delete()")
	void softDeleteUser_DeletedEntityHasCorrectRole() {
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).delete(captor.capture());
		assertThat(captor.getValue().getRole()).isEqualTo(ROLE);
	}

	@Test
	@DisplayName("should pass entity with isDeleted = false — @SQLDelete handles the DB update")
	void softDeleteUser_EntityPassedToDeleteHasIsDeletedFalse() {
		// IMPORTANT: isDeleted is still false on the Java entity at this point.
		// The actual soft-delete (UPDATE SET is_deleted = true) is triggered by
		// the @SQLDelete annotation at the Hibernate level — not by this service.
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).delete(captor.capture());
		assertThat(captor.getValue().isDeleted()).isFalse();
	}

	@Test
	@DisplayName("should pass entity with null deletedTimestamp — set by @SQLDelete at DB level")
	void softDeleteUser_EntityPassedToDeleteHasNullDeletedTimestamp() {
		// deletedTimestamp is set by the DB via @SQLDelete — not by this service.
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).delete(captor.capture());
		assertThat(captor.getValue().getDeletedTimestamp()).isNull();
	}

	// ─── Not-found / error cases ──────────────────────────────────────────────

	@Test
	@DisplayName("should throw ResourceNotFoundException when user does not exist")
	void softDeleteUser_UserNotFound_ThrowsResourceNotFoundException() {
		stubNotFound(USER_ID);

		assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("should include the searched id in the exception message")
	void softDeleteUser_UserNotFound_ExceptionContainsId() {
		stubNotFound(USER_ID);

		assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(String.valueOf(USER_ID));
	}

	@Test
	@DisplayName("should never call delete() when user is not found")
	void softDeleteUser_UserNotFound_DeleteNeverCalled() {
		stubNotFound(USER_ID);

		assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(userRepository, never()).delete(any(User.class));
	}

	@Test
	@DisplayName("should never call save() when user is not found")
	void softDeleteUser_UserNotFound_SaveNeverCalled() {
		stubNotFound(USER_ID);

		assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("should call getUserOrThrow() exactly once even when user is not found")
	void softDeleteUser_UserNotFound_GetUserOrThrowCalledOnce() {
		stubNotFound(USER_ID);

		assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(getActiveUser, times(1)).getUserOrThrow(USER_ID);
	}

	// ─── Parameterized ───────────────────────────────────────────────────────

	@ParameterizedTest(name = "non-existent id = {0}")
	@ValueSource(longs = {99L, 999L, Long.MAX_VALUE})
	@DisplayName("should throw ResourceNotFoundException for any non-existent id")
	void softDeleteUser_NonExistentIds_ThrowsResourceNotFoundException(long nonExistentId) {
		given(getActiveUser.getUserOrThrow(nonExistentId))
				.willThrow(new ResourceNotFoundException("User not found with id: " + nonExistentId));

		assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(nonExistentId))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(userRepository, never()).delete(any(User.class));
	}

	@ParameterizedTest(name = "valid id = {0}")
	@ValueSource(longs = {1L, 50L, 100L, Long.MAX_VALUE})
	@DisplayName("should pass the exact id to getUserOrThrow and delete the returned entity")
	void softDeleteUser_PassesCorrectIdToGetUserOrThrow(long id) {
		User user = User.builder()
				.id(id).email(EMAIL).password("encoded").role(ROLE)
				.isDeleted(false).createdAt(CREATED_AT).updatedAt(UPDATED_AT)
				.createdById(0L).updatedById(0L)
				.build();

		given(getActiveUser.getUserOrThrow(id)).willReturn(user);

		softDeleteUserService.softDeleteUser(id);

		verify(getActiveUser).getUserOrThrow(id);
		verify(userRepository).delete(user);
	}

	@Test
	@DisplayName("should throw ResourceNotFoundException when id = 0")
	void softDeleteUser_ZeroId_ThrowsResourceNotFoundException() {
		given(getActiveUser.getUserOrThrow(0L))
				.willThrow(new ResourceNotFoundException("User not found with id: 0"));

		assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(0L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("should throw ResourceNotFoundException when id is negative")
	void softDeleteUser_NegativeId_ThrowsResourceNotFoundException() {
		given(getActiveUser.getUserOrThrow(-1L))
				.willThrow(new ResourceNotFoundException("User not found with id: -1"));

		assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(-1L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	// ─── Ordering & exception propagation ────────────────────────────────────

	@Test
	@DisplayName("should call getUserOrThrow() BEFORE delete()")
	void softDeleteUser_FetchesBeforeDeleting() {
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		var inOrder = inOrder(getActiveUser, userRepository);
		inOrder.verify(getActiveUser).getUserOrThrow(USER_ID);
		inOrder.verify(userRepository).delete(any(User.class));
	}

	@Test
	@DisplayName("should propagate RuntimeException thrown by getUserOrThrow()")
	void softDeleteUser_GetUserOrThrowThrows_PropagatesException() {
		given(getActiveUser.getUserOrThrow(USER_ID))
				.willThrow(new RuntimeException("DB connection lost"));

		assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("DB connection lost");
	}

	@Test
	@DisplayName("should never call delete() when getUserOrThrow() throws")
	void softDeleteUser_GetUserOrThrowThrows_DeleteNeverCalled() {
		given(getActiveUser.getUserOrThrow(USER_ID))
				.willThrow(new RuntimeException("DB connection lost"));

		assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID))
				.isInstanceOf(RuntimeException.class);

		verify(userRepository, never()).delete(any(User.class));
	}

	@Test
	@DisplayName("should propagate RuntimeException thrown by delete()")
	void softDeleteUser_DeleteThrows_PropagatesException() {
		stubFound();
		willThrow(new RuntimeException("Delete failed"))
				.given(userRepository).delete(any(User.class));

		assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Delete failed");
	}

	@Test
	@DisplayName("should propagate DataIntegrityViolationException from delete()")
	void softDeleteUser_DeleteThrowsDataIntegrity_Propagates() {
		stubFound();
		willThrow(new org.springframework.dao.DataIntegrityViolationException("Constraint violation"))
				.given(userRepository).delete(any(User.class));

		assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	// ─── @SQLDelete contract ──────────────────────────────────────────────────

	@Test
	@DisplayName("should delegate to repository.delete() — @SQLDelete rewrites to UPDATE at DB level")
	void softDeleteUser_DelegatesDeleteToRepository() {
		// The service's job is only to call repository.delete(user).
		// The @SQLDelete annotation on User entity handles the actual SQL rewrite:
		//   UPDATE users SET um_is_deleted = true, um_deleted_timestamp = CURRENT_TIMESTAMP
		//   WHERE um_id = ?
		// This is transparent to the service layer and verified only in integration tests.
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		verify(userRepository, times(1)).delete(existingUser);
	}

	@Test
	@DisplayName("should not manually set isDeleted or deletedTimestamp — that is @SQLDelete's job")
	void softDeleteUser_DoesNotManuallySetIsDeletedOrTimestamp() {
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).delete(captor.capture());

		assertThat(captor.getValue().isDeleted())
				.as("Service must not manually set isDeleted — @SQLDelete handles this")
				.isFalse();
		assertThat(captor.getValue().getDeletedTimestamp())
				.as("Service must not manually set deletedTimestamp — @SQLDelete handles this")
				.isNull();
	}

	@Test
	@DisplayName("should use getUserOrThrow() to exclude already-deleted users")
	void softDeleteUser_UsesActiveUserQuery_ExcludesAlreadyDeletedUsers() {
		// getUserOrThrow() internally calls findActiveById(), which filters
		// out soft-deleted rows — preventing a double soft-delete
		stubFound();

		softDeleteUserService.softDeleteUser(USER_ID);

		verify(getActiveUser).getUserOrThrow(USER_ID);
		verify(userRepository, never()).findById(any());
	}
}