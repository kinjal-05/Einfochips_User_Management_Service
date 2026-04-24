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
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - updateUser()")
@ActiveProfiles("test")
public class UpdateUserServiceTest {
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
	private UpdateUserServiceImpl updateUserService;

	private static final long          USER_ID        = 1L;
	private static final String        ORIGINAL_EMAIL = "original@example.com";
	private static final String        UPDATED_EMAIL  = "updated@example.com";
	private static final Role ORIGINAL_ROLE  = Role.ROLE_USER;
	private static final Role          UPDATED_ROLE   = Role.ROLE_ADMIN;
	private static final LocalDateTime CREATED_AT     = LocalDateTime.of(2024, 1, 1, 10, 0);
	private static final LocalDateTime UPDATED_AT     = LocalDateTime.of(2024, 6, 1, 10, 0);
	private User existingUser;
	private User                 savedUser;
	private UserUpdateRequestDTO validRequest;

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

	private void stubNotFound(long id) {

		given(getActiveUser.getUserOrThrow(id))
				.willThrow(new ResourceNotFoundException(
						"User not found with id: " + id
				));
	}

	@Test
	@Order(1)
	@DisplayName("should return a non-null UserResponseDTO on success")
	void updateUser_HappyPath_ReturnsNonNullDTO() {
		stubFoundAndSaved();

		UserResponseDTO result = updateUserService.updateUser(USER_ID, validRequest);

		assertThat(result).isNotNull();
	}

	@Test
	@Order(2)
	@DisplayName("should return DTO with the id from the saved entity")
	void updateUser_ReturnsCorrectId() {
		stubFoundAndSaved();

		UserResponseDTO result = updateUserService.updateUser(USER_ID, validRequest);

		assertThat(result.id()).isEqualTo(USER_ID);
	}

	@Test
	@Order(3)
	@DisplayName("should return DTO with the email from the saved entity")
	void updateUser_ReturnsEmailFromSavedEntity() {
		stubFoundAndSaved();

		UserResponseDTO result = updateUserService.updateUser(USER_ID, validRequest);

		assertThat(result.email()).isEqualTo(UPDATED_EMAIL);
	}

	@Test
	@Order(4)
	@DisplayName("should return DTO with the role from the saved entity")
	void updateUser_ReturnsRoleFromSavedEntity() {
		stubFoundAndSaved();

		UserResponseDTO result = updateUserService.updateUser(USER_ID, validRequest);

		assertThat(result.role()).isEqualTo(UPDATED_ROLE);
	}

	@Test
	@Order(5)
	@DisplayName("should return DTO with createdAt from the saved entity")
	void updateUser_ReturnsCreatedAt() {
		stubFoundAndSaved();

		UserResponseDTO result = updateUserService.updateUser(USER_ID, validRequest);

		assertThat(result.createdAt()).isEqualTo(CREATED_AT);
	}

	@Test
	@Order(6)
	@DisplayName("should return DTO with updatedAt from the saved entity")
	void updateUser_ReturnsUpdatedAt() {
		stubFoundAndSaved();

		UserResponseDTO result = updateUserService.updateUser(USER_ID, validRequest);

		assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
	}

	@Test
	@Order(7)
	@DisplayName("should call getActiveUser exactly once")
	void updateUser_CallsGetActiveUserExactlyOnce() {

		stubFoundAndSaved();

		updateUserService.updateUser(USER_ID, validRequest);

		verify(getActiveUser, times(1)).getUserOrThrow(USER_ID);
	}

	@Test
	@Order(8)
	@DisplayName("should call userRepository.save() exactly once")
	void updateUser_CallsSaveExactlyOnce() {
		stubFoundAndSaved();

		updateUserService.updateUser(USER_ID, validRequest);

		verify(userRepository, times(1)).save(any(User.class));
	}

	@Test
	@Order(9)
	@DisplayName("should invoke no extra dependencies beyond getUserOrThrow, save and mapper")
	void updateUser_NoExtraRepositoryInteractions() {

		stubFoundAndSaved();

		updateUserService.updateUser(USER_ID, validRequest);

		// verify correct service flow
		verify(getActiveUser, times(1)).getUserOrThrow(USER_ID);
		verify(userRepository, times(1)).save(any(User.class));
		verify(mapToUserResponseDTO, times(1)).mapToUserResponseDTO(any(User.class));

		// ensure no extra interactions anywhere
		verifyNoMoreInteractions(getActiveUser, userRepository, mapToUserResponseDTO);
	}

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

	@Test
	@Order(14)
	@DisplayName("should pass the same entity returned by findActiveById to save()")
	void updateUser_PassesFetchedEntityToSave() {
		stubFoundAndSaved();

		updateUserService.updateUser(USER_ID, validRequest);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		// The entity passed to save must be the one returned by getUserOrThrow
		assertThat(captor.getValue().getId()).isEqualTo(existingUser.getId());
	}

	@Test
	@Order(15)
	@DisplayName("should throw ResourceNotFoundException when user does not exist")
	void updateUser_UserNotFound_ThrowsResourceNotFoundException() {
		stubNotFound(USER_ID);

		assertThatThrownBy(() -> updateUserService.updateUser(USER_ID, validRequest))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@Order(16)
	@DisplayName("should include the id in the exception message")
	void updateUser_UserNotFound_ExceptionMessageContainsId() {
		stubNotFound(USER_ID);

		assertThatThrownBy(() -> updateUserService.updateUser(USER_ID, validRequest))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(String.valueOf(USER_ID));
	}

	@Test
	@Order(17)
	@DisplayName("should never call save() when user is not found")
	void updateUser_UserNotFound_SaveNeverCalled() {
		stubNotFound(USER_ID);

		assertThatThrownBy(() -> updateUserService.updateUser(USER_ID, validRequest))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(userRepository, never()).save(any());
	}

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

	@Order(21)
	@ParameterizedTest(name = "valid id = {0}")
	@ValueSource(longs = {1L, 50L, 100L, Long.MAX_VALUE})
	@DisplayName("should pass the exact id to GetActiveUser")
	void updateUser_PassesCorrectIdToGetActiveUser(long id) {

		User user = User.builder()
				.id(id)
				.email(ORIGINAL_EMAIL)
				.role(ORIGINAL_ROLE)
				.isDeleted(false)
				.createdAt(CREATED_AT)
				.updatedAt(CREATED_AT)
				.createdById(0L)
				.updatedById(0L)
				.build();

		User saved = User.builder()
				.id(id)
				.email(UPDATED_EMAIL)
				.role(UPDATED_ROLE)
				.isDeleted(false)
				.createdAt(CREATED_AT)
				.updatedAt(UPDATED_AT)
				.createdById(0L)
				.updatedById(id)
				.build();

		given(getActiveUser.getUserOrThrow(id)).willReturn(user);
		given(userRepository.save(any(User.class))).willReturn(saved);

		updateUserService.updateUser(id, validRequest);

		verify(getActiveUser).getUserOrThrow(id);
	}
	@Test
	@Order(22)
	@DisplayName("should propagate DataIntegrityViolationException from save()")
	void updateUser_SaveThrowsDataIntegrity_Propagates() {

		given(getActiveUser.getUserOrThrow(USER_ID))
				.willReturn(existingUser);

		given(userRepository.save(any(User.class)))
				.willThrow(new DataIntegrityViolationException("Duplicate email"));

		assertThatThrownBy(() -> updateUserService.updateUser(USER_ID, validRequest))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("Duplicate email");
	}

	@Test
	@Order(23)
	@DisplayName("should propagate OptimisticLockingFailureException from save()")
	void updateUser_SaveThrowsOptimisticLocking_Propagates() {

		given(getActiveUser.getUserOrThrow(USER_ID))
				.willReturn(existingUser);

		given(userRepository.save(any(User.class)))
				.willThrow(new OptimisticLockingFailureException("Version conflict"));

		assertThatThrownBy(() -> updateUserService.updateUser(USER_ID, validRequest))
				.isInstanceOf(OptimisticLockingFailureException.class)
				.hasMessageContaining("Version conflict");
	}

	@Test
	@Order(24)
	@DisplayName("should propagate RuntimeException from save()")
	void updateUser_SaveThrowsRuntimeException_Propagates() {

		given(getActiveUser.getUserOrThrow(USER_ID))
				.willReturn(existingUser);

		given(userRepository.save(any(User.class)))
				.willThrow(new RuntimeException("Unexpected DB error"));

		assertThatThrownBy(() -> updateUserService.updateUser(USER_ID, validRequest))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Unexpected DB error");
	}

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

	@Test
	@Order(28)
	@DisplayName("should reflect DB-enriched data (audit fields) from the saved entity")
	void updateUser_ReflectsDbEnrichedAuditFields() {

		User dbEnriched = User.builder()
				.id(USER_ID)
				.email(UPDATED_EMAIL)
				.role(UPDATED_ROLE)
				.isDeleted(false)
				.createdAt(CREATED_AT)
				.updatedAt(UPDATED_AT)
				.createdById(0L)
				.updatedById(USER_ID)
				.build();

		given(getActiveUser.getUserOrThrow(USER_ID))
				.willReturn(existingUser);

		given(userRepository.save(any(User.class)))
				.willReturn(dbEnriched);

		given(mapToUserResponseDTO.mapToUserResponseDTO(dbEnriched))
				.willReturn(new UserResponseDTO(
						dbEnriched.getId(),
						dbEnriched.getEmail(),
						dbEnriched.getRole(),
						dbEnriched.getCreatedAt(),
						dbEnriched.getUpdatedAt(),
						dbEnriched.getCreatedById(),
						dbEnriched.getUpdatedById()
				));

		UserResponseDTO result = updateUserService.updateUser(USER_ID, validRequest);

		assertThat(result).isNotNull();
		assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
		assertThat(result.updatedById()).isEqualTo(USER_ID);
	}

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
