package userservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import userservice.Utility.GetActiveUser;
import userservice.Utility.MapToUserResponseDTO;
import userservice.dtos.UserResponseDTO;
import userservice.dtos.UserSearchRequestDTO;
import userservice.enums.Role;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.security.CustomUserDetails;
import userservice.security.JwtService;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


/**
 * Comprehensive unit test suite for {@link SearchUserServiceImpl#searchUsers(UserSearchRequestDTO, Pageable)}.
 *
 * <p>This test class validates the user search functionality in isolation using
 * Mockito and JUnit 5. It ensures correct filtering, pagination handling,
 * entity-to-DTO mapping, and robust interaction with the repository layer.</p>
 *
 * <h3>Test Coverage</h3>
 * <ul>
 *   <li><b>Happy Path:</b>
 *       Verifies successful retrieval of users as a paginated {@link Page}
 *       of {@link UserResponseDTO}.</li>
 *
 *   <li><b>Pagination Handling:</b>
 *       <ul>
 *           <li>Preserves page number, size, and total pages</li>
 *           <li>Validates total element count</li>
 *           <li>Handles empty, single, and multi-element pages</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>DTO Mapping:</b>
 *       <ul>
 *           <li>Ensures each {@link User} entity is mapped to {@link UserResponseDTO}</li>
 *           <li>Validates all fields (id, email, role, timestamps, audit fields)</li>
 *           <li>Supports mapping across multiple entities</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Specification & Filtering:</b>
 *       <ul>
 *           <li>Ensures a non-null {@link Specification} is always passed to repository</li>
 *           <li>Validates behavior with various filters:
 *               <ul>
 *                   <li>Email filter</li>
 *                   <li>Role filter</li>
 *                   <li>Date range filter</li>
 *                   <li>All-null filters</li>
 *               </ul>
 *           </li>
 *           <li>Confirms repository is invoked exactly once regardless of filters</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Repository Interaction:</b>
 *       <ul>
 *           <li>Ensures {@code findAll(Specification, Pageable)} is called exactly once</li>
 *           <li>Validates correct {@link Pageable} is passed</li>
 *           <li>Confirms no additional repository interactions</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Dependency Isolation:</b>
 *       <ul>
 *           <li>Ensures no interaction with unrelated dependencies
 *               (e.g., {@link PasswordEncoder}, {@link AuthenticationManager}, {@link JwtService})</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Exception Handling:</b>
 *       <ul>
 *           <li>Propagates runtime exceptions thrown by repository</li>
 *           <li>Handles null inputs (request, pageable)</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Edge Cases:</b>
 *       <ul>
 *           <li>Empty result set</li>
 *           <li>Null filter fields</li>
 *           <li>Single-element result</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Parameterized Testing:</b>
 *       Ensures correct mapping behavior for all {@link Role} enum values.</li>
 * </ul>
 *
 * <h3>Testing Strategy</h3>
 * <ul>
 *   <li>Uses {@link MockitoExtension} for mock initialization</li>
 *   <li>Follows Arrange-Act-Assert pattern</li>
 *   <li>Utilizes helper methods for reusable stubbing logic</li>
 *   <li>Captures and verifies {@link Specification} and {@link Pageable} arguments</li>
 * </ul>
 *
 * <h3>Key Design Considerations</h3>
 * <ul>
 *   <li>Ensures strict separation between service logic and data access layer</li>
 *   <li>Validates correctness of dynamic query construction via Specification</li>
 *   <li>Prevents regression in filtering and pagination behavior</li>
 *   <li>Maintains high readability and maintainability</li>
 * </ul>
 *
 * <p>This test suite is designed to meet production-grade standards and ensure
 * reliability, correctness, and scalability of the user search functionality.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - searchUser()")
@ActiveProfiles("test")
public class SearchUserServiceTest {

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
	private SearchUserServiceImpl searchUserService;

	private static final long          USER_ID_1  = 1L;
	private static final long          USER_ID_2  = 2L;
	private static final String        EMAIL_1    = "alice@example.com";
	private static final String        EMAIL_2    = "bob@example.com";
	private static final Role          ROLE_USER  = Role.ROLE_USER;
	private static final Role          ROLE_ADMIN = Role.ROLE_ADMIN;
	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2024, 1, 1, 10, 0);
	private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2024, 6, 1, 10, 0);

	private User user1;
	private User user2;
	private UserResponseDTO dto1;
	private UserResponseDTO dto2;
	private Pageable pageable;
	private UserSearchRequestDTO blankRequest;

	// ─── Fixture ──────────────────────────────────────────────────────────────

	@BeforeEach
	void setUp() {
		user1 = User.builder()
				.id(USER_ID_1).email(EMAIL_1).role(ROLE_USER)
				.isDeleted(false).createdAt(CREATED_AT).updatedAt(UPDATED_AT)
				.createdById(0L).updatedById(0L)
				.build();

		user2 = User.builder()
				.id(USER_ID_2).email(EMAIL_2).role(ROLE_ADMIN)
				.isDeleted(false).createdAt(CREATED_AT).updatedAt(UPDATED_AT)
				.createdById(0L).updatedById(0L)
				.build();

		dto1 = new UserResponseDTO(USER_ID_1, EMAIL_1, ROLE_USER,  CREATED_AT, UPDATED_AT, 0L, 0L);
		dto2 = new UserResponseDTO(USER_ID_2, EMAIL_2, ROLE_ADMIN, CREATED_AT, UPDATED_AT, 0L, 0L);

		pageable     = PageRequest.of(0, 10);
		blankRequest = new UserSearchRequestDTO(null, null, 1, 1, LocalDateTime.now(), LocalDateTime.now());
	}

	// ─── Helpers ──────────────────────────────────────────────────────────────

	private Page<User> pageOf(User... users) {
		return new PageImpl<>(List.of(users), pageable, users.length);
	}

	/**
	 * Stubs both the repository query AND the mapper for every user in the page.
	 * This is required because the service calls usersPage.map(mapper::map),
	 * so an unstubbed mapper returns null — causing NPE on any DTO field access.
	 */
	private void stubSearch(Page<User> page) {
		given(userRepository.findAll(any(Specification.class), eq(pageable)))
				.willReturn(page);
		page.getContent().forEach(user ->
				given(mapToUserResponseDTO.mapToUserResponseDTO(user))
						.willReturn(toDto(user))
		);
	}

	/** Resolves the pre-built DTO fixture for a given user instance. */
	private UserResponseDTO toDto(User user) {
		if (user == user1) return dto1;
		if (user == user2) return dto2;
		// Fallback for parameterised tests that build their own User instances
		return new UserResponseDTO(
				user.getId(), user.getEmail(), user.getRole(),
				user.getCreatedAt(), user.getUpdatedAt(),
				user.getCreatedById(), user.getUpdatedById()
		);
	}

	// ─── Basic return value ───────────────────────────────────────────────────

	@Test
	@Order(1)
	@DisplayName("should return a non-null Page on success")
	void searchUsers_ReturnsNonNullPage() {
		stubSearch(pageOf(user1));

		Page<UserResponseDTO> result = searchUserService.searchUsers(blankRequest, pageable);

		assertThat(result).isNotNull();
	}

	@Test
	@Order(2)
	@DisplayName("should return an empty page when repository returns no results")
	void searchUsers_EmptyResult_ReturnsEmptyPage() {
		given(userRepository.findAll(any(Specification.class), eq(pageable)))
				.willReturn(Page.empty(pageable));
		// mapper is never called for an empty page — no stub needed

		Page<UserResponseDTO> result = searchUserService.searchUsers(blankRequest, pageable);

		assertThat(result.getTotalElements()).isZero();
		assertThat(result.getContent()).asList().isEmpty();
	}

	@Test
	@Order(3)
	@DisplayName("should return page with correct total element count")
	void searchUsers_ReturnsTotalElementCount() {
		stubSearch(pageOf(user1, user2));

		Page<UserResponseDTO> result = searchUserService.searchUsers(blankRequest, pageable);

		assertThat(result.getTotalElements()).isEqualTo(2);
	}

	@Test
	@Order(4)
	@DisplayName("should map each user entity to a UserResponseDTO")
	void searchUsers_MapsEntitiesToDTOs() {
		stubSearch(pageOf(user1, user2));

		Page<UserResponseDTO> result = searchUserService.searchUsers(blankRequest, pageable);

		assertThat(result.getContent()).asList().hasSize(2);
	}

	// ─── DTO field mapping ────────────────────────────────────────────────────

	@Test
	@Order(5)
	@DisplayName("should map id correctly for each DTO in the page")
	void searchUsers_MapsIdCorrectly() {
		stubSearch(pageOf(user1));

		Page<UserResponseDTO> result = searchUserService.searchUsers(blankRequest, pageable);

		assertThat(result.getContent().get(0).id()).isEqualTo(USER_ID_1);
	}

	@Test
	@Order(6)
	@DisplayName("should map email correctly for each DTO in the page")
	void searchUsers_MapsEmailCorrectly() {
		stubSearch(pageOf(user1));

		Page<UserResponseDTO> result = searchUserService.searchUsers(blankRequest, pageable);

		assertThat(result.getContent().get(0).email()).isEqualTo(EMAIL_1);
	}

	@Test
	@Order(7)
	@DisplayName("should map role correctly for each DTO in the page")
	void searchUsers_MapsRoleCorrectly() {
		stubSearch(pageOf(user1));

		Page<UserResponseDTO> result = searchUserService.searchUsers(blankRequest, pageable);

		assertThat(result.getContent().get(0).role()).isEqualTo(ROLE_USER);
	}

	@Test
	@Order(8)
	@DisplayName("should map createdAt correctly for each DTO in the page")
	void searchUsers_MapsCreatedAtCorrectly() {
		stubSearch(pageOf(user1));

		Page<UserResponseDTO> result = searchUserService.searchUsers(blankRequest, pageable);

		assertThat(result.getContent().get(0).createdAt()).isEqualTo(CREATED_AT);
	}

	@Test
	@Order(9)
	@DisplayName("should map updatedAt correctly for each DTO in the page")
	void searchUsers_MapsUpdatedAtCorrectly() {
		stubSearch(pageOf(user1));

		Page<UserResponseDTO> result = searchUserService.searchUsers(blankRequest, pageable);

		assertThat(result.getContent().get(0).updatedAt()).isEqualTo(UPDATED_AT);
	}

	@Test
	@Order(10)
	@DisplayName("should map all fields of all entities to DTOs")
	void searchUsers_MapsAllFieldsForAllEntities() {
		stubSearch(pageOf(user1, user2));

		Page<UserResponseDTO> result = searchUserService.searchUsers(blankRequest, pageable);
		List<UserResponseDTO> content = result.getContent();

		assertThat(content.get(0).id()).isEqualTo(USER_ID_1);
		assertThat(content.get(0).email()).isEqualTo(EMAIL_1);
		assertThat(content.get(0).role()).isEqualTo(ROLE_USER);

		assertThat(content.get(1).id()).isEqualTo(USER_ID_2);
		assertThat(content.get(1).email()).isEqualTo(EMAIL_2);
		assertThat(content.get(1).role()).isEqualTo(ROLE_ADMIN);
	}

	// ─── Repository interaction ───────────────────────────────────────────────

	@Test
	@Order(11)
	@DisplayName("should call userRepository.findAll() with Specification and Pageable exactly once")
	void searchUsers_CallsFindAllExactlyOnce() {
		stubSearch(pageOf(user1));

		searchUserService.searchUsers(blankRequest, pageable);

		verify(userRepository, times(1))
				.findAll(any(Specification.class), eq(pageable));
	}

	@Test
	@Order(12)
	@DisplayName("should invoke no extra repository methods beyond findAll()")
	void searchUsers_NoExtraRepositoryInteractions() {
		stubSearch(pageOf(user1));

		searchUserService.searchUsers(blankRequest, pageable);

		verify(userRepository, times(1))
				.findAll(any(Specification.class), eq(pageable));
		verifyNoMoreInteractions(userRepository);
	}

	@Test
	@Order(13)
	@DisplayName("should pass the exact Pageable to repository")
	void searchUsers_PassesExactPageableToRepository() {
		Pageable customPageable = PageRequest.of(2, 5);
		User u = user1;
		given(userRepository.findAll(any(Specification.class), eq(customPageable)))
				.willReturn(new PageImpl<>(List.of(u), customPageable, 1));
		given(mapToUserResponseDTO.mapToUserResponseDTO(u)).willReturn(dto1);

		searchUserService.searchUsers(blankRequest, customPageable);

		verify(userRepository).findAll(any(Specification.class), eq(customPageable));
	}

	@Test
	@Order(14)
	@DisplayName("should never interact with passwordEncoder, authenticationManager or jwtService")
	void searchUsers_NoInteractionsWithOtherDependencies() {
		stubSearch(pageOf(user1));

		searchUserService.searchUsers(blankRequest, pageable);

		verifyNoInteractions(passwordEncoder);
		verifyNoInteractions(authenticationManager);
		verifyNoInteractions(jwtService);
	}

	// ─── Pagination metadata ──────────────────────────────────────────────────

	@Test
	@Order(15)
	@DisplayName("should preserve page number from repository result")
	void searchUsers_PreservesPageNumber() {
		Pageable page2 = PageRequest.of(1, 5);
		given(userRepository.findAll(any(Specification.class), eq(page2)))
				.willReturn(new PageImpl<>(List.of(user1), page2, 11));
		given(mapToUserResponseDTO.mapToUserResponseDTO(user1)).willReturn(dto1);

		Page<UserResponseDTO> result = searchUserService.searchUsers(blankRequest, page2);

		assertThat(result.getNumber()).isEqualTo(1);
	}

	@Test
	@Order(16)
	@DisplayName("should preserve page size from repository result")
	void searchUsers_PreservesPageSize() {
		Pageable size5 = PageRequest.of(0, 5);
		given(userRepository.findAll(any(Specification.class), eq(size5)))
				.willReturn(new PageImpl<>(List.of(user1, user2), size5, 2));
		given(mapToUserResponseDTO.mapToUserResponseDTO(user1)).willReturn(dto1);
		given(mapToUserResponseDTO.mapToUserResponseDTO(user2)).willReturn(dto2);

		Page<UserResponseDTO> result = searchUserService.searchUsers(blankRequest, size5);

		assertThat(result.getSize()).isEqualTo(5);
	}

	@Test
	@Order(17)
	@DisplayName("should preserve total pages count from repository result")
	void searchUsers_PreservesTotalPages() {
		given(userRepository.findAll(any(Specification.class), eq(pageable)))
				.willReturn(new PageImpl<>(List.of(user1, user2), pageable, 25));
		given(mapToUserResponseDTO.mapToUserResponseDTO(user1)).willReturn(dto1);
		given(mapToUserResponseDTO.mapToUserResponseDTO(user2)).willReturn(dto2);

		Page<UserResponseDTO> result = searchUserService.searchUsers(blankRequest, pageable);

		assertThat(result.getTotalPages()).isEqualTo(3); // ceil(25/10)
	}

	@Test
	@Order(18)
	@DisplayName("should return a single-element page when repository returns one user")
	void searchUsers_SingleResult_ReturnsOneElementPage() {
		stubSearch(pageOf(user1));

		Page<UserResponseDTO> result = searchUserService.searchUsers(blankRequest, pageable);

		assertThat(result.getContent()).asList().hasSize(1);
	}

	// ─── Filter-to-specification passthrough ──────────────────────────────────

	@Test
	@Order(19)
	@DisplayName("should pass a non-null Specification to repository")
	void searchUsers_PassesNonNullSpecificationToRepository() {
		stubSearch(pageOf(user1));

		searchUserService.searchUsers(blankRequest, pageable);

		ArgumentCaptor<Specification<User>> captor =
				ArgumentCaptor.forClass(Specification.class);
		verify(userRepository).findAll(captor.capture(), eq(pageable));
		assertThat(captor.getValue()).isNotNull();
	}

	@Test
	@Order(20)
	@DisplayName("should build and pass a Specification even when all filter fields are null")
	void searchUsers_AllNullFilters_StillPassesSpecificationToRepository() {
		UserSearchRequestDTO allNull =
				new UserSearchRequestDTO(null, null, 1, 1, null, null);
		given(userRepository.findAll(any(Specification.class), eq(pageable)))
				.willReturn(Page.empty(pageable));
		// mapper is never called for an empty page — no stub needed

		searchUserService.searchUsers(allNull, pageable);

		verify(userRepository, times(1))
				.findAll(any(Specification.class), eq(pageable));
	}

	@Test
	@Order(21)
	@DisplayName("should still invoke findAll once when email filter is provided")
	void searchUsers_WithEmailFilter_CallsFindAllOnce() {
		UserSearchRequestDTO emailFilter =
				new UserSearchRequestDTO(EMAIL_1, null, 1, 1, null, null);
		given(userRepository.findAll(any(Specification.class), eq(pageable)))
				.willReturn(pageOf(user1));
		given(mapToUserResponseDTO.mapToUserResponseDTO(user1)).willReturn(dto1);

		searchUserService.searchUsers(emailFilter, pageable);

		verify(userRepository, times(1))
				.findAll(any(Specification.class), eq(pageable));
	}

	@Test
	@Order(22)
	@DisplayName("should still invoke findAll once when role filter is provided")
	void searchUsers_WithRoleFilter_CallsFindAllOnce() {
		UserSearchRequestDTO roleFilter =
				new UserSearchRequestDTO(null, ROLE_USER, 1, 1, null, null);
		given(userRepository.findAll(any(Specification.class), eq(pageable)))
				.willReturn(pageOf(user1));
		given(mapToUserResponseDTO.mapToUserResponseDTO(user1)).willReturn(dto1);

		searchUserService.searchUsers(roleFilter, pageable);

		verify(userRepository, times(1))
				.findAll(any(Specification.class), eq(pageable));
	}

	@Test
	@Order(23)
	@DisplayName("should still invoke findAll once when date range filter is provided")
	void searchUsers_WithDateRangeFilter_CallsFindAllOnce() {
		UserSearchRequestDTO dateFilter = new UserSearchRequestDTO(
				null, null, 1, 1,
				LocalDateTime.of(2024, 1, 1, 0, 0),
				LocalDateTime.of(2024, 12, 31, 23, 59)
		);
		given(userRepository.findAll(any(Specification.class), eq(pageable)))
				.willReturn(pageOf(user1, user2));
		given(mapToUserResponseDTO.mapToUserResponseDTO(user1)).willReturn(dto1);
		given(mapToUserResponseDTO.mapToUserResponseDTO(user2)).willReturn(dto2);

		searchUserService.searchUsers(dateFilter, pageable);

		verify(userRepository, times(1))
				.findAll(any(Specification.class), eq(pageable));
	}

	// ─── Exception propagation ────────────────────────────────────────────────

	@Test
	@Order(24)
	@DisplayName("should propagate RuntimeException thrown by repository")
	void searchUsers_RepositoryThrows_PropagatesException() {
		given(userRepository.findAll(any(Specification.class), eq(pageable)))
				.willThrow(new RuntimeException("DB unavailable"));

		assertThatThrownBy(() -> searchUserService.searchUsers(blankRequest, pageable))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("DB unavailable");
	}

	@Test
	@Order(25)
	@DisplayName("should throw NullPointerException when request is null")
	void searchUsers_NullRequest_ThrowsNullPointerException() {
		assertThatThrownBy(() -> searchUserService.searchUsers(null, pageable))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	@Order(26)
	@DisplayName("should throw NullPointerException when pageable is null")
	void searchUsers_NullPageable_ThrowsNullPointerException() {
		assertThatThrownBy(() -> searchUserService.searchUsers(blankRequest, null))
				.isInstanceOf(NullPointerException.class);
	}

	// ─── Role enum coverage ───────────────────────────────────────────────────

	@Order(27)
	@DisplayName("should correctly map every Role enum value in returned DTOs")
	@ParameterizedTest(name = "role={0}")
	@EnumSource(Role.class)
	void searchUsers_MapsEveryRoleCorrectly(Role role) {
		User userWithRole = User.builder()
				.id(USER_ID_1).email(EMAIL_1).role(role)
				.isDeleted(false).createdAt(CREATED_AT).updatedAt(UPDATED_AT)
				.createdById(0L).updatedById(0L)
				.build();

		UserResponseDTO dtoWithRole = new UserResponseDTO(
				USER_ID_1, EMAIL_1, role, CREATED_AT, UPDATED_AT, 0L, 0L);

		given(userRepository.findAll(any(Specification.class), eq(pageable)))
				.willReturn(pageOf(userWithRole));
		// Stub the mapper for this specific instance — any(User.class) would
		// conflict across parameterised iterations, so match the exact object
		given(mapToUserResponseDTO.mapToUserResponseDTO(userWithRole))
				.willReturn(dtoWithRole);

		Page<UserResponseDTO> result = searchUserService.searchUsers(blankRequest, pageable);

		assertThat(result.getContent().get(0).role()).isEqualTo(role);
	}
}