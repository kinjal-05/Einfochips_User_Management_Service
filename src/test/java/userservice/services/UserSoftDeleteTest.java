package userservice.services;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
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

import userservice.enums.Role;
import userservice.exceptions.ResourceNotFoundException;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.security.CustomUserDetails;
import userservice.security.JwtService;
import userservice.services.impls.UserSoftDeleteServiceImpl;
import userservice.utility.GetActiveUser;
import userservice.utility.MapToUserResponseDTO;

/**
 * Unit test suite for {@link UserSoftDeleteServiceImpl#softDeleteUser(long)}.
 *
 * <h2>Purpose</h2> Validates the service-layer contract for soft-deleting a
 * user. All persistence and infrastructure concerns are mocked — this suite
 * tests <em>behaviour</em>, not SQL execution.
 *
 * <h2>Design Under Test</h2>
 * <ul>
 * <li>User lookup is delegated to {@link GetActiveUser#getUserOrThrow(long)},
 * which internally queries only active (non-deleted) rows.</li>
 * <li>Deletion is delegated to {@link UserRepository#delete(Object)}.</li>
 * <li>The actual soft-delete SQL
 * ({@code UPDATE … SET is_deleted = true, deleted_timestamp = NOW()}) is
 * triggered by the {@code @SQLDelete} annotation on the {@link User} entity —
 * completely transparent to this service layer.</li>
 * <li>The method is {@code void}; no return value is asserted.</li>
 * </ul>
 *
 * <h2>What Is NOT Tested Here</h2>
 * <ul>
 * <li>Actual SQL / Hibernate {@code @SQLDelete} rewrite → integration
 * tests.</li>
 * <li>Database constraint enforcement → integration tests.</li>
 * </ul>
 *
 * <h2>Test Organisation</h2> Tests are grouped with {@link Nested} classes by
 * concern:
 * <ol>
 * <li>Happy path</li>
 * <li>Repository interaction counts</li>
 * <li>Unrelated dependency isolation</li>
 * <li>Argument capture assertions</li>
 * <li>Not-found and error cases</li>
 * <li>Boundary / parameterized edge cases</li>
 * <li>Invocation ordering and exception propagation</li>
 * <li>{@code @SQLDelete} contract</li>
 * </ol>
 *
 * <h2>Testing Strategy</h2>
 * <ul>
 * <li>Behaviour verification via Mockito</li>
 * <li>Interaction-based assertions (verify calls and order)</li>
 * <li>Exception propagation validation</li>
 * <li>Argument capture to validate the exact entity passed to the
 * repository</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserSoftDeleteServiceImpl — softDeleteUser()")
@ActiveProfiles("test")
class UserSoftDeleteTest {

	// ─── Mocked dependencies ──────────────────────────────────────────────────

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

	/**
	 * Class under test. Mockito injects the mocks above via constructor injection.
	 */
	@InjectMocks
	private UserSoftDeleteServiceImpl softDeleteUserService;

	// ─── Shared test fixtures ─────────────────────────────────────────────────

	private static final long USER_ID = 1L;
	private static final String EMAIL = "john.doe@example.com";
	private static final Role ROLE = Role.ROLE_USER;
	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2024, 1, 1, 10, 0);
	private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2024, 6, 1, 12, 0);

	/**
	 * Reusable active {@link User} entity. Rebuilt before every test via
	 * {@link #setUp()} to guarantee test isolation.
	 */
	private User existingUser;

	// ─── Lifecycle ────────────────────────────────────────────────────────────

	/**
	 * Rebuilds {@link #existingUser} before every test. Keeping fixture
	 * construction here avoids shared mutable state across tests.
	 */
	@BeforeEach
	void setUp() {
		existingUser = User.builder().id(USER_ID).email(EMAIL).password("encodedPassword").role(ROLE).isDeleted(false)
				.createdAt(CREATED_AT).updatedAt(UPDATED_AT).createdById(0L).updatedById(0L).build();
	}

	// ─── Shared stub helpers ──────────────────────────────────────────────────

	/**
	 * Stubs a successful active-user lookup for {@link #USER_ID}.
	 *
	 * <p>
	 * The service delegates lookup to {@link GetActiveUser#getUserOrThrow(long)};
	 * we stub the utility directly. Repository interactions are asserted separately
	 * only where explicitly tested.
	 *
	 * <p>
	 * <b>Note:</b> {@code softDeleteUser()} is {@code void} and never maps to a
	 * DTO, so {@code mapToUserResponseDTO} is intentionally not stubbed here.
	 */
	private void stubFound() {
		given(getActiveUser.getUserOrThrow(USER_ID)).willReturn(existingUser);
	}

	/**
	 * Stubs a failed active-user lookup for the supplied {@code id}, simulating a
	 * missing or already-deleted user.
	 *
	 * @param id the user id that should be treated as non-existent
	 */
	private void stubNotFound(long id) {
		given(getActiveUser.getUserOrThrow(id))
				.willThrow(new ResourceNotFoundException("User not found with id: " + id));
	}

	/**
	 * Convenience helper: runs {@code softDeleteUser()} and captures the
	 * {@link User} argument passed to {@link UserRepository#delete}.
	 *
	 * <p>
	 * Centralises the ArgumentCaptor boilerplate so individual tests can focus on a
	 * single assertion.
	 *
	 * @return the {@link User} entity that was handed to {@code delete()}
	 */
	private User captureDeletedUser() {
		softDeleteUserService.softDeleteUser(USER_ID);
		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).delete(captor.capture());
		return captor.getValue();
	}

	// =========================================================================
	// 1. Happy path
	// =========================================================================

	/**
	 * Verifies the primary success scenario: a valid user id is supplied, the user
	 * is found, and the operation completes without any exception.
	 */
	@Nested
	@DisplayName("1. Happy path")
	class HappyPath {

		/**
		 * Asserts the method completes without throwing any exception and that
		 * {@code delete()} is called exactly once.
		 */
		@Test
		@DisplayName("completes without exception when user exists")
		void softDeleteUser_HappyPath_NoExceptionThrown() {
			stubFound();

			assertThatNoException().isThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID));

			verify(userRepository).delete(any(User.class));
		}

		/**
		 * Asserts the method has no return value. Reaching the line after the call
		 * confirms normal completion.
		 */
		@Test
		@DisplayName("returns void — method has no return value")
		void softDeleteUser_ReturnsVoid() {
			stubFound();

			softDeleteUserService.softDeleteUser(USER_ID);

			// Void methods are validated by verifying interactions, not return values.
			assertThat(true).isTrue();
		}
	}

	// =========================================================================
	// 2. Repository interaction counts
	// =========================================================================

	/**
	 * Verifies the exact number of times each repository method is called. Prevents
	 * regressions where extra or missing calls are introduced.
	 */
	@Nested
	@DisplayName("2. Repository interaction counts")
	class RepositoryInteractionCounts {

		/**
		 * Asserts {@code getUserOrThrow()} is invoked exactly once with the correct
		 * user id — no redundant lookups should occur.
		 */
		@Test
		@DisplayName("calls getUserOrThrow() exactly once with the given id")
		void softDeleteUser_CallsGetUserOrThrowExactlyOnce() {
			stubFound();

			softDeleteUserService.softDeleteUser(USER_ID);

			verify(getActiveUser, times(1)).getUserOrThrow(USER_ID);
		}

		/**
		 * Asserts {@code delete()} is invoked exactly once. Duplicate deletes would
		 * cause inconsistent persistence state.
		 */
		@Test
		@DisplayName("calls delete() exactly once")
		void softDeleteUser_CallsDeleteExactlyOnce() {
			stubFound();

			softDeleteUserService.softDeleteUser(USER_ID);

			verify(userRepository, times(1)).delete(any(User.class));
		}

		/**
		 * Asserts no additional repository methods are called beyond {@code delete()}.
		 * Catches unintended side effects such as unexpected flush or count calls.
		 */
		@Test
		@DisplayName("invokes no extra repository methods beyond delete()")
		void softDeleteUser_NoExtraRepositoryInteractions() {
			stubFound();

			softDeleteUserService.softDeleteUser(USER_ID);

			verify(userRepository, times(1)).delete(any(User.class));
			verifyNoMoreInteractions(userRepository);
		}

		/**
		 * Asserts {@code save()} is never called. The soft-delete contract must not
		 * persist the entity via save — deletion is handled exclusively by
		 * {@code repository.delete()}.
		 */
		@Test
		@DisplayName("never calls save() on the repository")
		void softDeleteUser_NeverCallsSave() {
			stubFound();

			softDeleteUserService.softDeleteUser(USER_ID);

			verify(userRepository, never()).save(any());
		}

		/**
		 * Asserts {@code findById()} is never called directly. The service must use
		 * {@code getUserOrThrow()} (which calls {@code findActiveById()}) to exclude
		 * already-deleted users.
		 */
		@Test
		@DisplayName("never calls findById() — only findActiveById() via getUserOrThrow()")
		void softDeleteUser_NeverCallsFindById() {
			stubFound();

			softDeleteUserService.softDeleteUser(USER_ID);

			verify(userRepository, never()).findById(any());
		}
	}

	// =========================================================================
	// 3. Unrelated dependency isolation
	// =========================================================================

	/**
	 * Verifies that dependencies unrelated to soft-deletion are never touched.
	 * Keeps the service focused and prevents accidental coupling.
	 */
	@Nested
	@DisplayName("3. Unrelated dependency isolation")
	class DependencyIsolation {

		/**
		 * Asserts that security-related beans are not accessed during soft deletion.
		 * Soft-deletion is a data operation and must not trigger authentication flows.
		 */
		@Test
		@DisplayName("never interacts with passwordEncoder, authenticationManager or jwtService")
		void softDeleteUser_NoInteractionsWithSecurityDependencies() {
			stubFound();

			softDeleteUserService.softDeleteUser(USER_ID);

			verifyNoInteractions(passwordEncoder, authenticationManager, jwtService);
		}

		/**
		 * Asserts that the DTO mapper is never invoked. {@code softDeleteUser()} is
		 * {@code void} — there is nothing to map.
		 */
		@Test
		@DisplayName("never interacts with mapToUserResponseDTO — method is void")
		void softDeleteUser_NoInteractionWithMapper() {
			stubFound();

			softDeleteUserService.softDeleteUser(USER_ID);

			verifyNoInteractions(mapToUserResponseDTO);
		}
	}

	// =========================================================================
	// 4. Argument capture assertions
	// =========================================================================

	/**
	 * Verifies the exact state of the {@link User} entity passed to
	 * {@link UserRepository#delete}. Ensures the service passes through the fetched
	 * entity without modifying it.
	 */
	@Nested
	@DisplayName("4. Argument capture — entity passed to delete()")
	class ArgumentCaptureAssertions {

		/**
		 * Asserts a non-null {@link User} is passed to {@code delete()}. Passing null
		 * would cause a NullPointerException in the repository.
		 */
		@Test
		@DisplayName("passes a non-null User to delete()")
		void softDeleteUser_DeleteReceivesNonNullUser() {
			stubFound();

			assertThat(captureDeletedUser()).isNotNull();
		}

		/**
		 * Asserts the exact entity returned by {@code getUserOrThrow()} is forwarded to
		 * {@code delete()} without substitution or cloning.
		 */
		@Test
		@DisplayName("passes the exact entity returned by getUserOrThrow()")
		void softDeleteUser_PassesFetchedEntityToDelete() {
			stubFound();

			User deleted = captureDeletedUser();

			assertThat(deleted.getId()).isEqualTo(USER_ID);
			assertThat(deleted.getEmail()).isEqualTo(EMAIL);
		}

		/**
		 * Asserts the entity's {@code id} field is preserved unchanged when passed to
		 * {@code delete()}.
		 */
		@Test
		@DisplayName("entity passed to delete() has correct id")
		void softDeleteUser_DeletedEntityHasCorrectId() {
			stubFound();

			assertThat(captureDeletedUser().getId()).isEqualTo(USER_ID);
		}

		/**
		 * Asserts the entity's {@code email} field is preserved unchanged when passed
		 * to {@code delete()}.
		 */
		@Test
		@DisplayName("entity passed to delete() has correct email")
		void softDeleteUser_DeletedEntityHasCorrectEmail() {
			stubFound();

			assertThat(captureDeletedUser().getEmail()).isEqualTo(EMAIL);
		}

		/**
		 * Asserts the entity's {@code role} field is preserved unchanged when passed to
		 * {@code delete()}.
		 */
		@Test
		@DisplayName("entity passed to delete() has correct role")
		void softDeleteUser_DeletedEntityHasCorrectRole() {
			stubFound();

			assertThat(captureDeletedUser().getRole()).isEqualTo(ROLE);
		}

		/**
		 * Asserts {@code isDeleted} is still {@code false} on the Java object at the
		 * time {@code delete()} is called.
		 *
		 * <p>
		 * <b>Important:</b> The service must NOT manually set {@code isDeleted = true}.
		 * The actual soft-delete ({@code UPDATE SET is_deleted = true}) is performed
		 * transparently by the {@code @SQLDelete} annotation at the Hibernate level.
		 */
		@Test
		@DisplayName("entity has isDeleted = false — @SQLDelete handles the DB update")
		void softDeleteUser_EntityPassedToDeleteHasIsDeletedFalse() {
			stubFound();

			assertThat(captureDeletedUser().isDeleted()).isFalse();
		}

		/**
		 * Asserts {@code deletedTimestamp} is {@code null} on the Java object at the
		 * time {@code delete()} is called.
		 *
		 * <p>
		 * <b>Important:</b> The timestamp is populated at the DB level via
		 * {@code @SQLDelete} — the service must not set it manually.
		 */
		@Test
		@DisplayName("entity has null deletedTimestamp — set by @SQLDelete at DB level")
		void softDeleteUser_EntityPassedToDeleteHasNullDeletedTimestamp() {
			stubFound();

			assertThat(captureDeletedUser().getDeletedTimestamp()).isNull();
		}
	}

	// =========================================================================
	// 5. Not-found and error cases
	// =========================================================================

	/**
	 * Verifies exception handling when the requested user does not exist or has
	 * already been soft-deleted.
	 */
	@Nested
	@DisplayName("5. Not-found and error cases")
	class NotFoundAndErrorCases {

		/**
		 * Asserts a {@link ResourceNotFoundException} is thrown when the user does not
		 * exist or is already soft-deleted.
		 */
		@Test
		@DisplayName("throws ResourceNotFoundException when user does not exist")
		void softDeleteUser_UserNotFound_ThrowsResourceNotFoundException() {
			stubNotFound(USER_ID);

			assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		/**
		 * Asserts the exception message includes the searched user id to aid debugging
		 * and API error responses.
		 */
		@Test
		@DisplayName("exception message contains the searched id")
		void softDeleteUser_UserNotFound_ExceptionContainsId() {
			stubNotFound(USER_ID);

			assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID))
					.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining(String.valueOf(USER_ID));
		}

		/**
		 * Asserts {@code delete()} is never called when the user is not found. Prevents
		 * phantom deletes against non-existent records.
		 */
		@Test
		@DisplayName("never calls delete() when user is not found")
		void softDeleteUser_UserNotFound_DeleteNeverCalled() {
			stubNotFound(USER_ID);

			assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID))
					.isInstanceOf(ResourceNotFoundException.class);

			verify(userRepository, never()).delete(any(User.class));
		}

		/**
		 * Asserts {@code save()} is never called when the user is not found.
		 */
		@Test
		@DisplayName("never calls save() when user is not found")
		void softDeleteUser_UserNotFound_SaveNeverCalled() {
			stubNotFound(USER_ID);

			assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID))
					.isInstanceOf(ResourceNotFoundException.class);

			verify(userRepository, never()).save(any());
		}

		/**
		 * Asserts {@code getUserOrThrow()} is still called exactly once even when it
		 * throws — no retry or fallback logic should exist.
		 */
		@Test
		@DisplayName("calls getUserOrThrow() exactly once even when user is not found")
		void softDeleteUser_UserNotFound_GetUserOrThrowCalledOnce() {
			stubNotFound(USER_ID);

			assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID))
					.isInstanceOf(ResourceNotFoundException.class);

			verify(getActiveUser, times(1)).getUserOrThrow(USER_ID);
		}
	}

	// =========================================================================
	// 6. Boundary and parameterized edge cases
	// =========================================================================

	/**
	 * Parameterized and boundary tests covering a range of id values to ensure
	 * consistent behaviour across the id space.
	 */
	@Nested
	@DisplayName("6. Boundary and parameterized edge cases")
	class BoundaryAndParameterizedCases {

		/**
		 * Asserts a {@link ResourceNotFoundException} is thrown for any id that does
		 * not correspond to an active user.
		 *
		 * @param nonExistentId a non-existent user id
		 */
		@ParameterizedTest(name = "non-existent id = {0}")
		@ValueSource(longs = { 99L, 999L, Long.MAX_VALUE })
		@DisplayName("throws ResourceNotFoundException for any non-existent id")
		void softDeleteUser_NonExistentIds_ThrowsResourceNotFoundException(long nonExistentId) {
			given(getActiveUser.getUserOrThrow(nonExistentId))
					.willThrow(new ResourceNotFoundException("User not found with id: " + nonExistentId));

			assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(nonExistentId))
					.isInstanceOf(ResourceNotFoundException.class);

			verify(userRepository, never()).delete(any(User.class));
		}

		/**
		 * Asserts the correct id is forwarded to {@code getUserOrThrow()} and the
		 * returned entity is passed verbatim to {@code delete()}.
		 *
		 * @param id a valid user id to test
		 */
		@ParameterizedTest(name = "valid id = {0}")
		@ValueSource(longs = { 1L, 50L, 100L, Long.MAX_VALUE })
		@DisplayName("passes the exact id to getUserOrThrow and deletes the returned entity")
		void softDeleteUser_PassesCorrectIdToGetUserOrThrow(long id) {
			User user = User.builder().id(id).email(EMAIL).password("encoded").role(ROLE).isDeleted(false)
					.createdAt(CREATED_AT).updatedAt(UPDATED_AT).createdById(0L).updatedById(0L).build();

			given(getActiveUser.getUserOrThrow(id)).willReturn(user);

			softDeleteUserService.softDeleteUser(id);

			verify(getActiveUser).getUserOrThrow(id);
			verify(userRepository).delete(user);
		}

		/**
		 * Asserts that id = 0 is treated as non-existent and throws
		 * {@link ResourceNotFoundException}. Zero is not a valid database id.
		 */
		@Test
		@DisplayName("throws ResourceNotFoundException when id = 0")
		void softDeleteUser_ZeroId_ThrowsResourceNotFoundException() {
			given(getActiveUser.getUserOrThrow(0L))
					.willThrow(new ResourceNotFoundException("User not found with id: 0"));

			assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(0L))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		/**
		 * Asserts that a negative id is treated as non-existent and throws
		 * {@link ResourceNotFoundException}. Negative ids are never assigned by the
		 * persistence layer.
		 */
		@Test
		@DisplayName("throws ResourceNotFoundException when id is negative")
		void softDeleteUser_NegativeId_ThrowsResourceNotFoundException() {
			given(getActiveUser.getUserOrThrow(-1L))
					.willThrow(new ResourceNotFoundException("User not found with id: -1"));

			assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(-1L))
					.isInstanceOf(ResourceNotFoundException.class);
		}
	}

	// =========================================================================
	// 7. Invocation ordering and exception propagation
	// =========================================================================

	/**
	 * Verifies the strict ordering of method calls and that exceptions thrown by
	 * collaborators are propagated without swallowing.
	 */
	@Nested
	@DisplayName("7. Invocation ordering and exception propagation")
	class OrderingAndExceptionPropagation {

		/**
		 * Asserts {@code getUserOrThrow()} is always called before {@code delete()}.
		 * Deleting without first fetching the user would bypass the active-user filter
		 * and risk double soft-deletes.
		 */
		@Test
		@DisplayName("calls getUserOrThrow() BEFORE delete()")
		void softDeleteUser_FetchesBeforeDeleting() {
			stubFound();

			softDeleteUserService.softDeleteUser(USER_ID);

			var ordered = inOrder(getActiveUser, userRepository);
			ordered.verify(getActiveUser).getUserOrThrow(USER_ID);
			ordered.verify(userRepository).delete(any(User.class));
		}

		/**
		 * Asserts that a {@link RuntimeException} thrown by {@code getUserOrThrow()}
		 * propagates to the caller unchanged — no silent swallowing.
		 */
		@Test
		@DisplayName("propagates RuntimeException thrown by getUserOrThrow()")
		void softDeleteUser_GetUserOrThrowThrows_PropagatesException() {
			given(getActiveUser.getUserOrThrow(USER_ID)).willThrow(new RuntimeException("DB connection lost"));

			assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID)).isInstanceOf(RuntimeException.class)
					.hasMessageContaining("DB connection lost");
		}

		/**
		 * Asserts {@code delete()} is never called when {@code getUserOrThrow()}
		 * throws. The failure must short-circuit the operation immediately.
		 */
		@Test
		@DisplayName("never calls delete() when getUserOrThrow() throws")
		void softDeleteUser_GetUserOrThrowThrows_DeleteNeverCalled() {
			given(getActiveUser.getUserOrThrow(USER_ID)).willThrow(new RuntimeException("DB connection lost"));

			assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID))
					.isInstanceOf(RuntimeException.class);

			verify(userRepository, never()).delete(any(User.class));
		}

		/**
		 * Asserts that a {@link RuntimeException} thrown by {@code delete()} propagates
		 * to the caller unchanged.
		 */
		@Test
		@DisplayName("propagates RuntimeException thrown by delete()")
		void softDeleteUser_DeleteThrows_PropagatesException() {
			stubFound();
			willThrow(new RuntimeException("Delete failed")).given(userRepository).delete(any(User.class));

			assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID)).isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Delete failed");
		}

		/**
		 * Asserts that a {@link DataIntegrityViolationException} thrown by
		 * {@code delete()} propagates to the caller. This can occur when FK constraints
		 * prevent deletion even with soft-delete in place.
		 */
		@Test
		@DisplayName("propagates DataIntegrityViolationException from delete()")
		void softDeleteUser_DeleteThrowsDataIntegrity_Propagates() {
			stubFound();
			willThrow(new DataIntegrityViolationException("Constraint violation")).given(userRepository)
					.delete(any(User.class));

			assertThatThrownBy(() -> softDeleteUserService.softDeleteUser(USER_ID))
					.isInstanceOf(DataIntegrityViolationException.class);
		}
	}

	// =========================================================================
	// 8. @SQLDelete contract
	// =========================================================================

	/**
	 * Verifies that the service correctly honours the {@code @SQLDelete} contract:
	 * it must only call {@code repository.delete()} and must never manually
	 * manipulate soft-delete fields.
	 *
	 * <p>
	 * The {@code @SQLDelete} annotation on {@link User} rewrites the DELETE
	 * statement to:
	 * 
	 * <pre>
	 *   UPDATE users
	 *   SET um_is_deleted = true, um_deleted_timestamp = CURRENT_TIMESTAMP
	 *   WHERE um_id = ?
	 * </pre>
	 * 
	 * This rewrite is transparent to the service and verified only in integration
	 * tests.
	 */
	@Nested
	@DisplayName("8. @SQLDelete contract")
	class SqlDeleteContract {

		/**
		 * Asserts the service delegates to {@code repository.delete()} exactly once
		 * with the fetched entity. The SQL rewrite is Hibernate's responsibility.
		 */
		@Test
		@DisplayName("delegates to repository.delete() — @SQLDelete rewrites to UPDATE at DB level")
		void softDeleteUser_DelegatesDeleteToRepository() {
			stubFound();

			softDeleteUserService.softDeleteUser(USER_ID);

			verify(userRepository, times(1)).delete(existingUser);
		}

		/**
		 * Asserts the service never manually sets {@code isDeleted} or
		 * {@code deletedTimestamp} on the entity before calling {@code delete()}. Doing
		 * so would duplicate the {@code @SQLDelete} logic and cause inconsistent state
		 * if the annotation is ever changed.
		 */
		@Test
		@DisplayName("does not manually set isDeleted or deletedTimestamp — that is @SQLDelete's job")
		void softDeleteUser_DoesNotManuallySetIsDeletedOrTimestamp() {
			stubFound();

			User deleted = captureDeletedUser();

			assertThat(deleted.isDeleted()).as("Service must not manually set isDeleted — @SQLDelete handles this")
					.isFalse();
			assertThat(deleted.getDeletedTimestamp())
					.as("Service must not manually set deletedTimestamp — @SQLDelete handles this").isNull();
		}

		/**
		 * Asserts that {@code getUserOrThrow()} (backed by {@code findActiveById()}) is
		 * used instead of {@code findById()}, preventing a double soft-delete on
		 * already-deleted users.
		 */
		@Test
		@DisplayName("uses getUserOrThrow() to exclude already-deleted users")
		void softDeleteUser_UsesActiveUserQuery_ExcludesAlreadyDeletedUsers() {
			stubFound();

			softDeleteUserService.softDeleteUser(USER_ID);

			verify(getActiveUser).getUserOrThrow(USER_ID);
			verify(userRepository, never()).findById(any());
		}
	}
}