package userservice.services;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.verification.NoMoreInteractions;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import userservice.dtos.*;
import userservice.enums.Role;
import userservice.exceptions.ResourceNotFoundException;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.security.CustomUserDetails;
import userservice.security.JwtService;
import userservice.servicesImpl.UserServiceImpl;


import java.util.List;
import java.util.Optional;


import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit 5 test suite for UserService.
 *
 * This test class validates:
 * - User registration
 * - Login with JWT token generation
 * - User update operations
 * - Soft deletion
 * - Password change
 * - Search with paging
 *
 * Uses Mockito for mocking dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - createUser()")
@ActiveProfiles("test")
class UserServiceTest {

	// Mocked dependencies
	@Mock private UserRepository userRepository;
	@Mock private PasswordEncoder passwordEncoder;

	// Service under test
	@InjectMocks private UserServiceImpl userService;

	// Constants used in tests
	private static final String DEFAULT_PASSWORD  = "Temp@12345";
	private static final String ENCODED_PASSWORD  = "$2a$10$encodedHashHere";
	private static final String TEST_EMAIL="kinjal@gmail.com";
	private static final long SAVED_USER_ID=1;
	private static final Role TEST_ROLE=Role.ROLE_USER;

	// Test data
	private UserRequestDTO userRequest;
	private User savedUser;


	/**
	 * Initialize common objects before each test.
	 */
	@BeforeEach
	void setUp() {
		userRequest=new UserRequestDTO(TEST_EMAIL,TEST_ROLE);
		savedUser=User.builder().id(SAVED_USER_ID).email(TEST_EMAIL).password(ENCODED_PASSWORD).role(TEST_ROLE).isDeleted(false).build();
	}

//	Test Cases For Create User API
	@Nested
	@DisplayName("Create User API Testing")
	class CreateUser{


		private UserResponseDTO executeCreateUser(UserRequestDTO request, User userToReturn) {
			given(passwordEncoder.encode(DEFAULT_PASSWORD)).willReturn(ENCODED_PASSWORD);
			given(userRepository.save(any(User.class))).willReturn(userToReturn);
			return userService.createUser(request);
		}

		private User captureSavedUser() {
			ArgumentCaptor<User> argumentCaptor = ArgumentCaptor.forClass(User.class);
			verify(userRepository).save(argumentCaptor.capture());
			return argumentCaptor.getValue();
		}
			@Test
			@Order(1)
			@DisplayName("Should create User and return DTO")
			void createUser_returnUserResponseDTO()
			{
				UserResponseDTO responseDTO=executeCreateUser(userRequest,savedUser);
				assertThat(responseDTO).isNotNull();
				assertThat(responseDTO.id()).isEqualTo(SAVED_USER_ID);
				assertThat(responseDTO.email()).isEqualTo(TEST_EMAIL);
				assertThat(responseDTO.role()).isEqualTo(TEST_ROLE);
			}

			@Test
			@Order(2)
			@DisplayName("Should always encode hard password")
			void createUser_AlwaysEncodePassword()
			{
				executeCreateUser(userRequest,savedUser);
				verify(passwordEncoder,times(1)).encode(DEFAULT_PASSWORD);
				verifyNoMoreInteractions(passwordEncoder);
			}

			@Test
			@Order(3)
			@DisplayName("Should save user with isDeleted false")
			void createUser_SetIsDeletedFalse()
			{
				executeCreateUser(userRequest,savedUser);
				User argumentCaptor=captureSavedUser();
				assertThat(argumentCaptor.isDeleted()).isFalse();
			}

			@Test
			@Order(4)
			@DisplayName("Should save user with encoded password")
			void createUser_SavedEncodedPassword()
			{
				executeCreateUser(userRequest,savedUser);
				User argumentCaptor=captureSavedUser();
				assertThat(argumentCaptor.getPassword()).isEqualTo(ENCODED_PASSWORD).isNotEqualTo(DEFAULT_PASSWORD);
			}

			@Test
			@Order(5)
			@DisplayName("Should save user with exact email from request")
			void createUser_SavedWithExactEmail()
			{
				executeCreateUser(userRequest,savedUser);
				User argumentCaptor=captureSavedUser();
				assertThat(argumentCaptor.getEmail()).isEqualTo(TEST_EMAIL);
			}

			@Test
			@Order(6)
			@DisplayName("Should save user with exact role")
			void createUser_SavedWithExactRole()
			{
				executeCreateUser(userRequest,savedUser);
				User argumentCaptor=captureSavedUser();
				assertThat(argumentCaptor.getRole()).isEqualTo(TEST_ROLE);
			}

			@Test
			@Order(7)
			@DisplayName("Should always call password encoder once")
			void createUser_CallPasswordEncoderOnlyOnce()
			{
				executeCreateUser(userRequest,savedUser);
				verify(passwordEncoder,times(1)).encode(DEFAULT_PASSWORD);
			}

			@Test
			@Order(8)
			@DisplayName("Should call user repository.save() only once")
			void createUser_CallUserRepoOnlyOnce()
			{
				executeCreateUser(userRequest,savedUser);
				verify(userRepository,times(1)).save(any(User.class));
			}

			@Test
			@Order(9)
			@DisplayName("Should not call any other repo method")
			void createUser_NotCallAnyOtherMethod()
			{
				executeCreateUser(userRequest,savedUser);
				verify(userRepository,times(1)).save(any(User.class));
				verifyNoMoreInteractions(userRepository);
			}

			@Order(10)
			@DisplayName("Should work for every enum type role")
			@ParameterizedTest(name = "role={0}")
			@EnumSource(Role.class)
			void createUser_WorkForEveryEnumRole(Role role)
			{
					UserRequestDTO userRequestDTO=new UserRequestDTO(TEST_EMAIL,role);
					User savedUser=User.builder().id(SAVED_USER_ID).email(TEST_EMAIL).password(ENCODED_PASSWORD).role(role).isDeleted(false).build();
					UserResponseDTO userResponseDTO= executeCreateUser(userRequestDTO,savedUser);
					assertThat(userResponseDTO.role()).isEqualTo(role);
					User argumentCaptor=captureSavedUser();
					assertThat(argumentCaptor.getRole()).isEqualTo(role);
			}

			@Order(11)
			@DisplayName("Checking edge cases for email")
			@ParameterizedTest(name="email=\"{0}\"")
			@ValueSource(strings = {
					"simple@example.com",
					"user+tag@sub.domain.io",
					"UPPERCASE@EXAMPLE.COM",
					"123numeric@domain.org",
					"dots.in.local@part.com"
			})
			void createUser_CheckingEMAILEdgeCases(String email)
			{
				UserRequestDTO userRequestDTO=new UserRequestDTO(email,TEST_ROLE);
				User savedUser=User.builder().id(SAVED_USER_ID).email(email).password(ENCODED_PASSWORD).role(TEST_ROLE).isDeleted(false).build();
				UserResponseDTO userResponseDTO=executeCreateUser(userRequestDTO,savedUser);
				assertThat(userResponseDTO.email()).isEqualTo(email);
				User argumentCaptor=captureSavedUser();
				assertThat(argumentCaptor.getEmail()).isEqualTo(email);
			}

			@Test
			@Order(12)
			@DisplayName("Fully Object is pass to the repo")
			void createUser_FullObjectStateInRepo()
			{
				executeCreateUser(userRequest,savedUser);
				User user=captureSavedUser();
				assertThat(user.getEmail()).isEqualTo(TEST_EMAIL);
				assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
				assertThat(user.getRole()).isEqualTo(TEST_ROLE);
				assertThat(user.isDeleted()).isFalse();
			}

			@Test
			@Order(13)
			@DisplayName("Should Never Pass Null User to the repo")
			void createUser_NeverPassNullToRepo()
			{
				executeCreateUser(userRequest,savedUser);
				User argumentCaptor=captureSavedUser();
				assertThat(argumentCaptor).isNotNull();
			}

			@Test
			@Order(14)
			@DisplayName("Should propogate Exception thrown by user rsponse")
			void createUser_PropogateExceptionFromUserResponse()
			{
				given(passwordEncoder.encode(DEFAULT_PASSWORD)).willReturn(ENCODED_PASSWORD);
				given(userRepository.save(any(User.class))).willThrow(new RuntimeException("DB unavailiable"));
				assertThatThrownBy(()->userService.createUser(userRequest)).isInstanceOf(RuntimeException.class).hasMessageContaining("DB unavailiable");
			}

			@Test
			@Order(15)
			@DisplayName("Should propogate exception thrown by password encoder")
			void createUser_PropogateExceptionFromPasswordEncoder()
			{
				given(passwordEncoder.encode(DEFAULT_PASSWORD)).willThrow(new RuntimeException("Encoding error"));
				assertThatThrownBy(()->userService.createUser(userRequest)).isInstanceOf(RuntimeException.class).hasMessageContaining("Encoding error");
				verifyNoMoreInteractions(userRepository);
			}

			@Test
			@Order(16)
			@DisplayName("Should never call repo when password encoding fail")
				void createUser_NeverCallRepoOnPasswordEncoderFail()
				{
					given(passwordEncoder.encode(DEFAULT_PASSWORD)).willThrow(new RuntimeException("Encoding failure"));
					assertThatThrownBy(()->userService.createUser(userRequest)).isInstanceOf(RuntimeException.class).hasMessageContaining("Encoding failure");
					verify(userRepository,never()).save(any(User.class));
				}

				@Test
				@Order(17)
				@DisplayName("Should propogate data integrity violation exceptio in duplicated email")
				void createUser_DataIntegrityExceptionOnDuplicateEmail()
				{
					given(passwordEncoder.encode(DEFAULT_PASSWORD)).willReturn(ENCODED_PASSWORD);
					given(userRepository.save(any(User.class))).willThrow(new DataIntegrityViolationException("Duplicate entry for email"));
					assertThatThrownBy(()->userService.createUser(userRequest)).isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("Duplicate entry for email");
				}

				@Test
				@Order(18)
				@DisplayName("Should throw null pointer exception when request is null")
				void createUser_NullPointerException()
				{
					assertThatThrownBy(()->userService.createUser(null)).isInstanceOf(NullPointerException.class);
				}

				@Test
				@Order(19)
				@DisplayName("Should encode password first before saving it into DB")
				void createUser_EncodePassFirstBeforeSavingInDB()
				{
					executeCreateUser(userRequest,savedUser);
					var inOrder=inOrder(passwordEncoder,userRepository);
					inOrder.verify(passwordEncoder).encode(DEFAULT_PASSWORD);
					inOrder.verify(userRepository).save(any(User.class));
				}

			@Test
			@Order(20)
			@DisplayName("should return the entity returned by the repository, not the one built internally")
			void createUser_ReturnsDTOMappedFromRepositoryResult() {
				User dbEnrichedUser = User.builder()
						.id(1)
						.email(TEST_EMAIL)
						.password(ENCODED_PASSWORD)
						.role(TEST_ROLE)
						.isDeleted(false)
						.build();
				UserResponseDTO result = executeCreateUser(userRequest,dbEnrichedUser);
				assertThat(result.id()).isEqualTo(1);
			}
		}

}