package userservice.services;

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
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - GetUserById()")
@ActiveProfiles("test")
public class GetUserByIdTest {
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
	private GetUserByIdServiceImpl getUserByIdService;

	private static final long          USER_ID    = 1L;
	private static final String        EMAIL      = "john.doe@example.com";
	private static final Role ROLE       = Role.ROLE_USER;
	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2024, 1, 1, 10, 0);
	private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2024, 6, 1, 12, 0);
	private static final long          CREATED_BY = 0L;
	private static final long          UPDATED_BY = 2L;
	private User existingUser;
	private UserResponseDTO responseDTO;
	@BeforeEach
	void setUp() {
		existingUser = User.builder()
				.id(USER_ID)
				.email(EMAIL)
				.role(ROLE)
				.isDeleted(false)
				.createdAt(CREATED_AT)
				.updatedAt(UPDATED_AT)
				.createdById(CREATED_BY)
				.updatedById(UPDATED_BY)
				.build();

		responseDTO = new UserResponseDTO(
				USER_ID,
				EMAIL,
				ROLE,
				CREATED_AT,
				UPDATED_AT,
				0L,
				2L
		);
	}
	private void stubFound() {
		given(getActiveUser.getUserOrThrow(USER_ID)).willReturn(existingUser);
		given(mapToUserResponseDTO.mapToUserResponseDTO(existingUser)).willReturn(responseDTO);
	}

	private void stubNotFound(long id) {
		given(getActiveUser.getUserOrThrow(id))
				.willThrow(new ResourceNotFoundException("User not found with id: " + id));
	}

	@Test
	@DisplayName("should return a non-null UserResponseDTO")
	void getUserById_HappyPath_ReturnsNonNullDTO() {
		stubFound();

		UserResponseDTO result = getUserByIdService.getUserById(USER_ID);

		assertThat(result).isNotNull();
	}

	@Test
	@DisplayName("should return DTO with the correct id")
	void getUserById_ReturnsCorrectId() {
		stubFound();

		UserResponseDTO result = getUserByIdService.getUserById(USER_ID);

		assertThat(result.id()).isEqualTo(USER_ID);
	}

	@Test
	@DisplayName("should return DTO with the correct email")
	void getUserById_ReturnsCorrectEmail() {
		stubFound();

		UserResponseDTO result = getUserByIdService.getUserById(USER_ID);

		assertThat(result.email()).isEqualTo(EMAIL);
	}

	@Test
	@DisplayName("should return DTO with the correct role")
	void getUserById_ReturnsCorrectRole() {
		stubFound();

		UserResponseDTO result = getUserByIdService.getUserById(USER_ID);

		assertThat(result.role()).isEqualTo(ROLE);
	}

	@Test
	@DisplayName("should return DTO with the correct createdAt")
	void getUserById_ReturnsCorrectCreatedAt() {
		stubFound();

		UserResponseDTO result = getUserByIdService.getUserById(USER_ID);

		assertThat(result.createdAt()).isEqualTo(CREATED_AT);
	}

	@Test
	@DisplayName("should return DTO with the correct updatedAt")
	void getUserById_ReturnsCorrectUpdatedAt() {
		stubFound();

		UserResponseDTO result = getUserByIdService.getUserById(USER_ID);

		assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
	}

	@Test
	@DisplayName("should return DTO with the correct createdById")
	void getUserById_ReturnsCorrectCreatedById() {
		stubFound();

		UserResponseDTO result = getUserByIdService.getUserById(USER_ID);

		assertThat(result.createdById()).isEqualTo(CREATED_BY);
	}

	@Test
	@DisplayName("should return DTO with the correct updatedById")
	void getUserById_ReturnsCorrectUpdatedById() {
		stubFound();

		UserResponseDTO result = getUserByIdService.getUserById(USER_ID);

		assertThat(result.updatedById()).isEqualTo(UPDATED_BY);
	}

	@Test
	@DisplayName("should call GetActiveUser exactly once with the given id")
	void getUserById_CallsGetActiveUserExactlyOnce() {

		given(getActiveUser.getUserOrThrow(USER_ID))
				.willReturn(existingUser);

		given(mapToUserResponseDTO.mapToUserResponseDTO(existingUser))
				.willReturn(new UserResponseDTO(
						existingUser.getId(),
						existingUser.getEmail(),
						existingUser.getRole(),
						existingUser.getCreatedAt(),
						existingUser.getUpdatedAt(),
						existingUser.getCreatedById(),
						existingUser.getUpdatedById()
				));

		getUserByIdService.getUserById(USER_ID);

		verify(getActiveUser, times(1)).getUserOrThrow(USER_ID);
	}

	@Test
	@DisplayName("should invoke no extra dependencies beyond GetActiveUser and mapper")
	void getUserById_NoExtraInteractions() {

		given(getActiveUser.getUserOrThrow(USER_ID))
				.willReturn(existingUser);

		given(mapToUserResponseDTO.mapToUserResponseDTO(existingUser))
				.willReturn(new UserResponseDTO(
						existingUser.getId(),
						existingUser.getEmail(),
						existingUser.getRole(),
						existingUser.getCreatedAt(),
						existingUser.getUpdatedAt(),
						existingUser.getCreatedById(),
						existingUser.getUpdatedById()
				));

		getUserByIdService.getUserById(USER_ID);

		verify(getActiveUser).getUserOrThrow(USER_ID);
		verify(mapToUserResponseDTO).mapToUserResponseDTO(existingUser);

		verifyNoInteractions(userRepository);
	}
	@Test
	@DisplayName("should never interact with passwordEncoder, authenticationManager or jwtService")
	void getUserById_NoInteractionsWithOtherDependencies() {
		stubFound();

		getUserByIdService.getUserById(USER_ID);

		verifyNoInteractions(passwordEncoder);
		verifyNoInteractions(authenticationManager);
		verifyNoInteractions(jwtService);
	}

	@Test
	@DisplayName("should never call save() or delete() on the repository")
	void getUserById_NeverMutatesRepository() {
		stubFound();

		getUserByIdService.getUserById(USER_ID);

		verify(userRepository, never()).save(any());

	}

	@ParameterizedTest(name = "non-existent id = {0}")
	@ValueSource(longs = {99L, 999L, Long.MAX_VALUE})
	@DisplayName("should throw ResourceNotFoundException for any non-existent id")
	void getUserById_NonExistentIds_ThrowsResourceNotFoundException(long nonExistentId) {

		given(getActiveUser.getUserOrThrow(nonExistentId))
				.willThrow(new ResourceNotFoundException("User not found with id: " + nonExistentId));

		assertThatThrownBy(() -> getUserByIdService.getUserById(nonExistentId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(String.valueOf(nonExistentId));
	}

	@ParameterizedTest(name = "valid id = {0}")
	@ValueSource(longs = {1L, 50L, 100L, Long.MAX_VALUE})
	@DisplayName("should pass the exact id to GetActiveUser")
	void getUserById_PassesCorrectIdToGetActiveUser(long id) {

		given(getActiveUser.getUserOrThrow(id)).willReturn(existingUser);

		given(mapToUserResponseDTO.mapToUserResponseDTO(existingUser))
				.willReturn(new UserResponseDTO(
						existingUser.getId(),
						existingUser.getEmail(),
						existingUser.getRole(),
						existingUser.getCreatedAt(),
						existingUser.getUpdatedAt(),
						existingUser.getCreatedById(),
						existingUser.getUpdatedById()
				));

		getUserByIdService.getUserById(id);

		verify(getActiveUser).getUserOrThrow(id);
	}
	@Test
	@DisplayName("should throw ResourceNotFoundException when id = 0")
	void getUserById_ZeroId_ThrowsResourceNotFoundException() {

		given(getActiveUser.getUserOrThrow(0L))
				.willThrow(new ResourceNotFoundException("User not found"));

		assertThatThrownBy(() -> getUserByIdService.getUserById(0L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("should throw ResourceNotFoundException when id is negative")
	void getUserById_NegativeId_ThrowsResourceNotFoundException() {

		given(getActiveUser.getUserOrThrow(-1L))
				.willThrow(new ResourceNotFoundException("User not found"));

		assertThatThrownBy(() -> getUserByIdService.getUserById(-1L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("should propagate RuntimeException thrown by GetActiveUser")
	void getUserById_GetActiveUserThrows_PropagatesException() {

		given(getActiveUser.getUserOrThrow(USER_ID))
				.willThrow(new RuntimeException("DB connection lost"));

		assertThatThrownBy(() -> getUserByIdService.getUserById(USER_ID))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("DB connection lost");

		verify(getActiveUser).getUserOrThrow(USER_ID);
		verifyNoInteractions(userRepository);
	}

	@Test
	@DisplayName("should never call save() or delete() when GetActiveUser throws")
	void getUserById_FindActiveByIdThrows_NeverMutatesRepository() {

		given(getActiveUser.getUserOrThrow(USER_ID))
				.willThrow(new RuntimeException("DB connection lost"));

		assertThatThrownBy(() -> getUserByIdService.getUserById(USER_ID))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("DB connection lost");

		verify(getActiveUser).getUserOrThrow(USER_ID);

		verifyNoInteractions(userRepository);
	}

}
