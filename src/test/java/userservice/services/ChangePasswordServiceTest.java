package userservice.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import userservice.Utility.GetActiveUser;
import userservice.Utility.MapToUserResponseDTO;
import userservice.dtos.ChangePasswordRequestDTO;
import userservice.enums.Role;
import userservice.exceptions.ResourceNotFoundException;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.security.CustomUserDetails;
import userservice.security.JwtService;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;


/**
 * Comprehensive unit test suite for {@link ChangePasswordServiceImpl#changePassword(ChangePasswordRequestDTO)}.
 *
 * <p>This test class validates the behavior of the change password functionality
 * in isolation using Mockito and JUnit 5. It ensures correctness, security,
 * and robustness of the password update workflow under various scenarios.</p>
 *
 * <h3>Test Coverage</h3>
 * <ul>
 *   <li><b>Happy Path:</b>
 *       Verifies successful password change when the user is authenticated,
 *       exists in the system, and provides the correct old password.</li>
 *
 *   <li><b>Authentication Validation:</b>
 *       Ensures proper handling when:
 *       <ul>
 *           <li>Authentication is null</li>
 *           <li>User is not authenticated</li>
 *           <li>Principal is anonymous</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>User Validation:</b>
 *       Confirms {@link ResourceNotFoundException} is thrown when the user
 *       cannot be found by email.</li>
 *
 *   <li><b>Password Validation:</b>
 *       Ensures {@link BadCredentialsException} is thrown when:
 *       <ul>
 *           <li>Old password does not match</li>
 *           <li>Invalid password inputs are provided</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Security Guarantees:</b>
 *       <ul>
 *           <li>Password is always stored in encoded (hashed) form</li>
 *           <li>Plain text passwords are never persisted</li>
 *           <li>Old password is verified before encoding new password</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Interaction Verification:</b>
 *       <ul>
 *           <li>Validates correct method invocation order</li>
 *           <li>Ensures no unnecessary interactions with dependencies</li>
 *           <li>Confirms exact invocation counts</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Failure & Exception Handling:</b>
 *       <ul>
 *           <li>Propagates repository and encoder exceptions correctly</li>
 *           <li>Prevents further execution on failure conditions</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Edge Cases:</b>
 *       <ul>
 *           <li>Null request handling</li>
 *           <li>Null password inputs</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>Testing Strategy</h3>
 * <ul>
 *   <li>Uses {@link MockitoExtension} for mock initialization</li>
 *   <li>Mocks static {@link SecurityContextHolder} for authentication context</li>
 *   <li>Follows Arrange-Act-Assert pattern</li>
 *   <li>Reusable helper methods for common stubbing logic</li>
 * </ul>
 *
 * <h3>Key Design Considerations</h3>
 * <ul>
 *   <li>Ensures strict isolation of service logic</li>
 *   <li>Focuses on behavior verification rather than implementation details</li>
 *   <li>Prevents regression in authentication and password handling logic</li>
 * </ul>
 *
 * <p>This test suite is designed to be maintainable, readable, and aligned with
 * production-grade testing standards.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - changePassword()")
@ActiveProfiles("test")
public class ChangePasswordServiceTest {
	@Mock
	private UserRepository userRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private AuthenticationManager authenticationManager;
	@Mock private JwtService jwtService;
	@Mock private Authentication authentication;
	@Mock private CustomUserDetails customUserDetails;
	@Mock private SecurityContext securityContext;
	@Mock private MapToUserResponseDTO mapToUserResponseDTO;;
	@Mock private GetActiveUser getActiveUser;
	// Service under test
	@InjectMocks
	private ChangePasswordServiceImpl changePasswordService;

	private static final long          USER_ID        = 1L;
	private static final String        LOGGED_IN_EMAIL = "john.doe@example.com";
	private static final String        OLD_PASSWORD    = "OldPass@123";
	private static final String        NEW_PASSWORD    = "NewPass@456";
	private static final String        ENCODED_OLD     = "$2a$10$encodedOldHash";
	private static final String        ENCODED_NEW     = "$2a$10$encodedNewHash";
	private static final Role ROLE            = Role.ROLE_USER;
	private static final LocalDateTime CREATED_AT      = LocalDateTime.of(2024, 1, 1, 10, 0);
	private static final LocalDateTime UPDATED_AT      = LocalDateTime.of(2024, 6, 1, 12, 0);

	// ─── Fixtures ─────────────────────────────────────────────────────────────

	private User existingUser;
	private ChangePasswordRequestDTO validRequest;
	private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;
	@BeforeEach
	void setUp() {
		existingUser = User.builder()
				.id(USER_ID)
				.email(LOGGED_IN_EMAIL)
				.password(ENCODED_OLD)
				.role(ROLE)
				.isDeleted(false)
				.createdAt(CREATED_AT)
				.updatedAt(UPDATED_AT)
				.createdById(0L)
				.updatedById(0L)
				.build();

		validRequest = new ChangePasswordRequestDTO(OLD_PASSWORD, NEW_PASSWORD);

		// Open the static mock ONCE per test in setUp and close in tearDown.
		// This avoids manually opening/closing it in every single test.
		mockedSecurityContextHolder = Mockito.mockStatic(SecurityContextHolder.class);
	}

	@AfterEach
	void tearDown() {
		// CRITICAL: always close MockedStatic — failing to do so leaks the mock
		// into subsequent tests and causes false failures.
		mockedSecurityContextHolder.close();
	}

	// ─── Shared stub helpers ──────────────────────────────────────────────────

	/**
	 * Stubs the full SecurityContextHolder chain that getCurrentUserEmail() uses:
	 *   SecurityContextHolder.getContext() → SecurityContext
	 *   securityContext.getAuthentication() → Authentication
	 *   authentication.isAuthenticated()   → true
	 *   authentication.getName()           → LOGGED_IN_EMAIL
	 */
	private void stubAuthenticatedUser() {
		mockedSecurityContextHolder
				.when(SecurityContextHolder::getContext)
				.thenReturn(securityContext);
		given(securityContext.getAuthentication()).willReturn(authentication);
		given(authentication.isAuthenticated()).willReturn(true);
		given(authentication.getName()).willReturn(LOGGED_IN_EMAIL);
	}

	/**
	 * Stubs the full happy path:
	 * authenticated → user found → old password matches → new password encoded → saved
	 */
	private void stubFullHappyPath() {
		stubAuthenticatedUser();
		given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.of(existingUser));
		given(passwordEncoder.matches(OLD_PASSWORD, ENCODED_OLD)).willReturn(true);
		given(passwordEncoder.encode(NEW_PASSWORD)).willReturn(ENCODED_NEW);
		given(userRepository.save(any(User.class))).willReturn(existingUser);
	}

	@Test
	@DisplayName("should complete without throwing any exception")
	void changePassword_HappyPath_NoExceptionThrown() {
		stubFullHappyPath();

		assertThatNoException().isThrownBy(
				() -> changePasswordService.changePassword(validRequest)
		);
	}

	@Test
	@DisplayName("should call findByEmail() with the logged-in user's email")
	void changePassword_CallsFindByEmailWithLoggedInEmail() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		verify(userRepository, times(1)).findByEmail(LOGGED_IN_EMAIL);
	}

	@Test
	@DisplayName("should call passwordEncoder.matches() with the old password and stored hash")
	void changePassword_CallsPasswordMatchesWithCorrectArgs() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		verify(passwordEncoder, times(1)).matches(OLD_PASSWORD, ENCODED_OLD);
	}

	@Test
	@DisplayName("should call passwordEncoder.encode() with the new password")
	void changePassword_CallsEncodeWithNewPassword() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		verify(passwordEncoder, times(1)).encode(NEW_PASSWORD);
	}

	@Test
	@DisplayName("should call userRepository.save() exactly once")
	void changePassword_CallsSaveExactlyOnce() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		verify(userRepository, times(1)).save(any(User.class));
	}

	@Test
	@DisplayName("should update the user's password to the encoded new password before saving")
	void changePassword_SetsEncodedNewPasswordOnUserBeforeSave() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		// After changePassword(), the user entity's password must be the encoded new one
		assertThat(existingUser.getPassword())
				.as("User password must be updated to the encoded new password")
				.isEqualTo(ENCODED_NEW);
	}

	@Test
	@DisplayName("should save the user with the new encoded password")
	void changePassword_SavesUserWithNewEncodedPassword() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		verify(userRepository).save(argThat(u ->
				u.getPassword().equals(ENCODED_NEW)
		));
	}

	@Test
	@DisplayName("should invoke no extra repository methods beyond findByEmail + save")
	void changePassword_NoExtraRepositoryInteractions() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		verify(userRepository, times(1)).findByEmail(LOGGED_IN_EMAIL);
		verify(userRepository, times(1)).save(any(User.class));
		verifyNoMoreInteractions(userRepository);
	}

	@Test
	@DisplayName("should never interact with authenticationManager or jwtService")
	void changePassword_NoInteractionsWithUnusedDependencies() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		verifyNoInteractions(authenticationManager);
		verifyNoInteractions(jwtService);
	}

	@Test
	@DisplayName("should throw BadCredentialsException when authentication is null")
	void changePassword_NullAuthentication_ThrowsBadCredentials() {
		mockedSecurityContextHolder
				.when(SecurityContextHolder::getContext)
				.thenReturn(securityContext);
		given(securityContext.getAuthentication()).willReturn(null);

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(BadCredentialsException.class)
				.hasMessageContaining("User is not authenticated");
	}

	@Test
	@DisplayName("should throw BadCredentialsException when user is not authenticated")
	void changePassword_NotAuthenticated_ThrowsBadCredentials() {
		mockedSecurityContextHolder
				.when(SecurityContextHolder::getContext)
				.thenReturn(securityContext);
		given(securityContext.getAuthentication()).willReturn(authentication);
		given(authentication.isAuthenticated()).willReturn(false);

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(BadCredentialsException.class)
				.hasMessageContaining("User is not authenticated");
	}

	@Test
	@DisplayName("should throw BadCredentialsException when principal is 'anonymousUser'")
	void changePassword_AnonymousUser_ThrowsBadCredentials() {
		mockedSecurityContextHolder
				.when(SecurityContextHolder::getContext)
				.thenReturn(securityContext);
		given(securityContext.getAuthentication()).willReturn(authentication);
		given(authentication.isAuthenticated()).willReturn(true);
		given(authentication.getName()).willReturn("anonymousUser");

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(BadCredentialsException.class)
				.hasMessageContaining("User is not authenticated");
	}

	@Test
	@DisplayName("should never call findByEmail when authentication is null")
	void changePassword_NullAuthentication_FindByEmailNeverCalled() {
		mockedSecurityContextHolder
				.when(SecurityContextHolder::getContext)
				.thenReturn(securityContext);
		given(securityContext.getAuthentication()).willReturn(null);

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(BadCredentialsException.class);

		verify(userRepository, never()).findByEmail(any());
	}

	@Test
	@DisplayName("should never call save() when not authenticated")
	void changePassword_NotAuthenticated_SaveNeverCalled() {
		mockedSecurityContextHolder
				.when(SecurityContextHolder::getContext)
				.thenReturn(securityContext);
		given(securityContext.getAuthentication()).willReturn(authentication);
		given(authentication.isAuthenticated()).willReturn(false);

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(BadCredentialsException.class);

		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("should call SecurityContextHolder.getContext() to resolve the logged-in user")
	void changePassword_CallsSecurityContextHolder() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		mockedSecurityContextHolder.verify(SecurityContextHolder::getContext, atLeastOnce());
	}

	@Test
	@DisplayName("should throw ResourceNotFoundException when no user found for logged-in email")
	void changePassword_UserNotFound_ThrowsResourceNotFoundException() {
		stubAuthenticatedUser();
		given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.empty());

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("should include the email in the ResourceNotFoundException message")
	void changePassword_UserNotFound_ExceptionContainsEmail() {
		stubAuthenticatedUser();
		given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.empty());

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(LOGGED_IN_EMAIL);
	}

	@Test
	@DisplayName("should never call passwordEncoder.matches() when user not found")
	void changePassword_UserNotFound_PasswordMatchesNeverCalled() {
		stubAuthenticatedUser();
		given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.empty());

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(passwordEncoder, never()).matches(any(), any());
	}

	@Test
	@DisplayName("should never call passwordEncoder.encode() when user not found")
	void changePassword_UserNotFound_EncodeNeverCalled() {
		stubAuthenticatedUser();
		given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.empty());

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(passwordEncoder, never()).encode(any());
	}

	@Test
	@DisplayName("should never call save() when user not found")
	void changePassword_UserNotFound_SaveNeverCalled() {
		stubAuthenticatedUser();
		given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.empty());

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("should throw BadCredentialsException when old password does not match")
	void changePassword_WrongOldPassword_ThrowsBadCredentials() {
		stubAuthenticatedUser();
		given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.of(existingUser));
		given(passwordEncoder.matches(OLD_PASSWORD, ENCODED_OLD)).willReturn(false);

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(BadCredentialsException.class)
				.hasMessageContaining("Old password is incorrect");
	}

	@Test
	@DisplayName("should never call passwordEncoder.encode() when old password is wrong")
	void changePassword_WrongOldPassword_EncodeNeverCalled() {
		stubAuthenticatedUser();
		given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.of(existingUser));
		given(passwordEncoder.matches(OLD_PASSWORD, ENCODED_OLD)).willReturn(false);

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(BadCredentialsException.class);

		verify(passwordEncoder, never()).encode(any());
	}

	@Test
	@DisplayName("should never call save() when old password is wrong")
	void changePassword_WrongOldPassword_SaveNeverCalled() {
		stubAuthenticatedUser();
		given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.of(existingUser));
		given(passwordEncoder.matches(OLD_PASSWORD, ENCODED_OLD)).willReturn(false);

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(BadCredentialsException.class);

		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("should not update the user's password when old password is wrong")
	void changePassword_WrongOldPassword_PasswordNotUpdated() {
		stubAuthenticatedUser();
		given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.of(existingUser));
		given(passwordEncoder.matches(OLD_PASSWORD, ENCODED_OLD)).willReturn(false);

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(BadCredentialsException.class);

		// Password on the entity must remain the original encoded old password
		assertThat(existingUser.getPassword())
				.as("Password must not be changed when old password verification fails")
				.isEqualTo(ENCODED_OLD);
	}

	@Test
	@DisplayName("should resolve email THEN find user THEN verify password THEN encode THEN save")
	void changePassword_CorrectInteractionOrder() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		var inOrder = inOrder(userRepository, passwordEncoder);
		inOrder.verify(userRepository).findByEmail(LOGGED_IN_EMAIL);
		inOrder.verify(passwordEncoder).matches(OLD_PASSWORD, ENCODED_OLD);
		inOrder.verify(passwordEncoder).encode(NEW_PASSWORD);
		inOrder.verify(userRepository).save(any(User.class));
	}

	@Test
	@DisplayName("should call matches() BEFORE encode()")
	void changePassword_MatchesBeforeEncode() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		var inOrder = inOrder(passwordEncoder);
		inOrder.verify(passwordEncoder).matches(OLD_PASSWORD, ENCODED_OLD);
		inOrder.verify(passwordEncoder).encode(NEW_PASSWORD);
	}

	@Test
	@DisplayName("should call encode() BEFORE save()")
	void changePassword_EncodesBeforeSaving() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		var inOrder = inOrder(passwordEncoder, userRepository);
		inOrder.verify(passwordEncoder).encode(NEW_PASSWORD);
		inOrder.verify(userRepository).save(any(User.class));
	}

	@Test
	@DisplayName("should propagate RuntimeException thrown by findByEmail")
	void changePassword_FindByEmailThrows_PropagatesException() {
		stubAuthenticatedUser();
		given(userRepository.findByEmail(LOGGED_IN_EMAIL))
				.willThrow(new RuntimeException("DB connection lost"));

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("DB connection lost");
	}

	@Test
	@DisplayName("should never call save() when findByEmail throws")
	void changePassword_FindByEmailThrows_SaveNeverCalled() {
		stubAuthenticatedUser();
		given(userRepository.findByEmail(LOGGED_IN_EMAIL))
				.willThrow(new RuntimeException("DB connection lost"));

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(RuntimeException.class);

		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("should propagate RuntimeException thrown by save()")
	void changePassword_SaveThrows_PropagatesException() {
		stubAuthenticatedUser();
		given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.of(existingUser));
		given(passwordEncoder.matches(OLD_PASSWORD, ENCODED_OLD)).willReturn(true);
		given(passwordEncoder.encode(NEW_PASSWORD)).willReturn(ENCODED_NEW);
		given(userRepository.save(any(User.class)))
				.willThrow(new RuntimeException("Save failed"));

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Save failed");
	}

	@Test
	@DisplayName("should propagate RuntimeException thrown by passwordEncoder.encode()")
	void changePassword_EncodeThrows_PropagatesException() {
		stubAuthenticatedUser();
		given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.of(existingUser));
		given(passwordEncoder.matches(OLD_PASSWORD, ENCODED_OLD)).willReturn(true);
		given(passwordEncoder.encode(NEW_PASSWORD))
				.willThrow(new RuntimeException("Encoding failed"));

		assertThatThrownBy(() -> changePasswordService.changePassword(validRequest))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Encoding failed");

		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("should call matches() exactly once")
	void changePassword_CallsMatchesExactlyOnce() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		verify(passwordEncoder, times(1)).matches(any(), any());
	}

	@Test
	@DisplayName("should call encode() exactly once")
	void changePassword_CallsEncodeExactlyOnce() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		verify(passwordEncoder, times(1)).encode(any());
	}

	@Test
	@DisplayName("should never encode the old password")
	void changePassword_NeverEncodesOldPassword() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		verify(passwordEncoder, never()).encode(OLD_PASSWORD);
		verify(passwordEncoder, times(1)).encode(NEW_PASSWORD);
	}

	@Test
	@DisplayName("should never call matches() with the new password")
	void changePassword_NeverMatchesNewPassword() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		verify(passwordEncoder, never()).matches(eq(NEW_PASSWORD), any());
	}

	@Test
	@DisplayName("should store the encoded new password — never plain text")
	void changePassword_StoresEncodedPassword_NeverPlainText() {
		stubFullHappyPath();

		changePasswordService.changePassword(validRequest);

		assertThat(existingUser.getPassword())
				.as("Stored password must be the BCrypt hash, never plain text")
				.isEqualTo(ENCODED_NEW)
				.isNotEqualTo(NEW_PASSWORD);
	}

	@Test
	@DisplayName("should throw NullPointerException when request is null")
	void changePassword_NullRequest_ThrowsException() {
		// SecurityContext is still needed — getCurrentUserEmail() is called first
		stubAuthenticatedUser();
		given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.of(existingUser));

		assertThatThrownBy(() -> changePasswordService.changePassword(null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	@DisplayName("should throw when old password is null (matches receives null)")
	void changePassword_NullOldPassword_ThrowsException() {
		ChangePasswordRequestDTO nullOldReq = new ChangePasswordRequestDTO(null, NEW_PASSWORD);

		stubAuthenticatedUser();
		given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.of(existingUser));
		given(passwordEncoder.matches(null, ENCODED_OLD)).willReturn(false);

		assertThatThrownBy(() -> changePasswordService.changePassword(nullOldReq))
				.isInstanceOf(BadCredentialsException.class)
				.hasMessageContaining("Old password is incorrect");
	}
}
