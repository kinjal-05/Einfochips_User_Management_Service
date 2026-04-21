package userservice.services;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.internal.verification.NoMoreInteractions;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.web.oauth2.resourceserver.OAuth2ResourceServerSecurityMarker;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
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


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
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
	@Mock private AuthenticationManager authenticationManager;
	@Mock private JwtService jwtService;
	@Mock private Authentication authentication;
	@Mock private CustomUserDetails customUserDetails;
	@Mock private SecurityContext securityContext;
	// Service under test
	@InjectMocks private UserServiceImpl userService;

//	Test Cases For Create User API
	@Nested
	@DisplayName("Create User API Testing")
	class CreateUser{
		private static final String DEFAULT_PASSWORD  = "Temp@12345";
		private static final String ENCODED_PASSWORD  = "$2a$10$encodedHashHere";
		private static final String TEST_EMAIL="kinjal@gmail.com";
		private static final long SAVED_USER_ID=1;
		private static final Role TEST_ROLE=Role.ROLE_USER;

		// Test data
		private UserRequestDTO userRequest;
		private User savedUser;

		@BeforeEach
		void setUp() {
			userRequest=new UserRequestDTO(TEST_EMAIL,TEST_ROLE);
			savedUser=User.builder().id(SAVED_USER_ID).email(TEST_EMAIL).password(ENCODED_PASSWORD).role(TEST_ROLE).isDeleted(false).build();
		}
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

	@Nested
	@DisplayName("Login User API Testing")
	class LoginUser
	{
		private static final Long   USER_ID       = 10L;
		private static final String TEST_EMAIL    = "john.doe@example.com";
		private static final String TEST_PASSWORD = "Secret@123";
		private static final String JWT_TOKEN     = "eyJhbGciOiJIUzI1NiJ9.test.token";
		private static final Role   TEST_ROLE     = Role.ROLE_USER;

		private LoginRequestDTO loginRequestDTO;
		private User            savedUser;

		@BeforeEach
		void setUp() {
			loginRequestDTO = new LoginRequestDTO(TEST_EMAIL, TEST_PASSWORD);

			savedUser = User.builder()
					.id(USER_ID)
					.email(TEST_EMAIL)
					.role(TEST_ROLE)
					.isDeleted(false)
					.build();
		}
		private void authenticate() {
			given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
					.willReturn(authentication);
			given(authentication.getPrincipal()).willReturn(customUserDetails);
			given(customUserDetails.getUser()).willReturn(savedUser);
			given(jwtService.generateToken(customUserDetails)).willReturn(JWT_TOKEN);
		}

		@Test
		@Order(1)
		@DisplayName("should return a fully populated LoginResponseDTO on success")
		void login_ReturnsFullDTO() {
			authenticate();
			LoginResponseDTO result=userService.login(loginRequestDTO);
			assertThat(result).isNotNull();
			assertThat(result.id()).isEqualTo(USER_ID);
			assertThat(result.email()).isEqualTo(TEST_EMAIL);
			assertThat(result.role()).isEqualTo(TEST_ROLE);
			assertThat(result.token()).isEqualTo(JWT_TOKEN);
			assertThat(result.message()).isEqualTo("Login Successful");
		}

		@Test
		@Order(2)
		@DisplayName("should return the JWT token generated by JwtService")
		void login_ReturnsTokenFromJwtService() {
			authenticate();
			LoginResponseDTO result=userService.login(loginRequestDTO);
			assertThat(result.token()).isEqualTo(JWT_TOKEN);
		}

		@Test
		@Order(3)
		@DisplayName("should return the user id from the authenticated principal")
		void login_ReturnsCorrectUserId() {
			authenticate();
			LoginResponseDTO result=userService.login(loginRequestDTO);
			assertThat(result.id()).isEqualTo(USER_ID);
		}

		@Test
		@Order(4)
		@DisplayName("should return the email from the authenticated User entity")
		void login_ReturnsCorrectEmail() {
			authenticate();
			LoginResponseDTO result=userService.login(loginRequestDTO);
			assertThat(result.email()).isEqualTo(TEST_EMAIL);
		}

		@Test
		@Order(5)
		@DisplayName("should return the role from the authenticated User entity")
		void login_ReturnsCorrectRole() {
			authenticate();
			LoginResponseDTO result=userService.login(loginRequestDTO);
			assertThat(result.role()).isEqualTo(TEST_ROLE);
		}

		@Test
		@Order(6)
		@DisplayName("should never return null")
		void login_NeverReturnsNull() {
			authenticate();
			LoginResponseDTO result=userService.login(loginRequestDTO);
			assertThat(result).isNotNull();
		}

		@Test
		@Order(7)
		@DisplayName("should pass email and password from the request to authenticationManager")
		void login_PassesCorrectCredentialsToAuthManager() {
			authenticate();
			userService.login(loginRequestDTO);
			ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
					ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
			verify(authenticationManager).authenticate(captor.capture());
			UsernamePasswordAuthenticationToken captured = captor.getValue();
			assertThat(captured.getPrincipal()).isEqualTo(TEST_EMAIL);
			assertThat(captured.getCredentials()).isEqualTo(TEST_PASSWORD);
		}

		@Test
		@Order(8)
		@DisplayName("should call authenticationManager.authenticate() exactly once")
		void login_CallsAuthManagerExactlyOnce() {
			authenticate();
			userService.login(loginRequestDTO);
			verify(authenticationManager, times(1))
					.authenticate(any(UsernamePasswordAuthenticationToken.class));
		}

		@Test
		@Order(9)
		@DisplayName("should cast authentication principal to CustomUserDetails")
		void login_CastsPrincipalToCustomUserDetails() {
			authenticate();
			userService.login(loginRequestDTO);
			// getPrincipal() must be called to obtain CustomUserDetails
			verify(authentication, times(1)).getPrincipal();
		}

		@Test
		@Order(10)
		@DisplayName("should call jwtService.generateToken() exactly once with CustomUserDetails")
		void login_CallsJwtServiceExactlyOnce() {
			authenticate();
			userService.login(loginRequestDTO);
			verify(jwtService, times(1)).generateToken(customUserDetails);
			verifyNoMoreInteractions(jwtService);
		}

		@Test
		@Order(11)
		@DisplayName("should pass the principal (CustomUserDetails) to jwtService, not the raw user")
		void login_PassesCustomUserDetailsTOJwtService() {
			authenticate();
			userService.login(loginRequestDTO);
			ArgumentCaptor<CustomUserDetails> captor =
					ArgumentCaptor.forClass(CustomUserDetails.class);
			verify(jwtService).generateToken(captor.capture());
			assertThat(captor.getValue()).isSameAs(customUserDetails);
		}

		@Test
		@Order(12)
		@DisplayName("should embed the JWT from jwtService directly in the response DTO")
		void login_TokenInDTOMatchesJwtServiceOutput() {
			String customToken = "custom.jwt.token.xyz";
			given(authenticationManager.authenticate(any())).willReturn(authentication);
			given(authentication.getPrincipal()).willReturn(customUserDetails);
			given(customUserDetails.getUser()).willReturn(savedUser);
			given(jwtService.generateToken(customUserDetails)).willReturn(customToken);

			LoginResponseDTO result = userService.login(loginRequestDTO);

			assertThat(result.token()).isEqualTo(customToken);
		}

		@Test
		@Order(13)
		@DisplayName("should authenticate BEFORE generating a JWT token")
		void login_AuthenticatesBeforeGeneratingToken() {
			authenticate();
			userService.login(loginRequestDTO);
			var inOrder = inOrder(authenticationManager, jwtService);
			inOrder.verify(authenticationManager).authenticate(any());
			inOrder.verify(jwtService).generateToken(any());
		}

		@Test
		@Order(14)
		@DisplayName("should get principal BEFORE generating a JWT token")
		void login_GetsPrincipalBeforeGeneratingToken() {
			authenticate();
			userService.login(loginRequestDTO);
			var inOrder = inOrder(authentication, jwtService);
			inOrder.verify(authentication).getPrincipal();
			inOrder.verify(jwtService).generateToken(any());
		}

		@Test
		@Order(15)
		@DisplayName("should propagate BadCredentialsException for wrong password")
		void login_WrongPassword_ThrowsBadCredentials() {
			given(authenticationManager.authenticate(any()))
					.willThrow(new BadCredentialsException("Bad credentials"));
			assertThatThrownBy(() -> userService.login(loginRequestDTO))
					.isInstanceOf(BadCredentialsException.class)
					.hasMessageContaining("Bad credentials");
		}

		@Test
		@Order(16)
		@DisplayName("should propagate BadCredentialsException for unknown email")
		void login_UnknownEmail_ThrowsBadCredentials() {
			LoginRequestDTO unknownRequest =
					new LoginRequestDTO("unknown@example.com", TEST_PASSWORD);
			given(authenticationManager.authenticate(any()))
					.willThrow(new BadCredentialsException("Bad credentials"));
			assertThatThrownBy(() -> userService.login(unknownRequest))
					.isInstanceOf(BadCredentialsException.class);
		}

		@Test
		@Order(17)
		@DisplayName("should propagate DisabledException when account is disabled")
		void login_DisabledAccount_ThrowsDisabledException() {
			given(authenticationManager.authenticate(any()))
					.willThrow(new DisabledException("Account disabled"));
			assertThatThrownBy(() -> userService.login(loginRequestDTO))
					.isInstanceOf(DisabledException.class)
					.hasMessageContaining("Account disabled");
		}

		@Test
		@Order(18)
		@DisplayName("should propagate LockedException when account is locked")
		void login_LockedAccount_ThrowsLockedException() {
			given(authenticationManager.authenticate(any()))
					.willThrow(new LockedException("Account locked"));
			assertThatThrownBy(() -> userService.login(loginRequestDTO))
					.isInstanceOf(LockedException.class)
					.hasMessageContaining("Account locked");
		}

		@Test
		@Order(19)
		@DisplayName("should NOT call jwtService when authentication fails")
		void login_AuthFails_JwtServiceNeverCalled() {
			given(authenticationManager.authenticate(any()))
					.willThrow(new BadCredentialsException("Bad credentials"));
			assertThatThrownBy(() -> userService.login(loginRequestDTO))
					.isInstanceOf(BadCredentialsException.class);
			verifyNoInteractions(jwtService);
		}

		@Test
		@Order(20)
		@DisplayName("should NOT call getPrincipal() when authentication fails")
		void login_AuthFails_GetPrincipalNeverCalled() {
			given(authenticationManager.authenticate(any()))
					.willThrow(new BadCredentialsException("Bad credentials"));

			assertThatThrownBy(() -> userService.login(loginRequestDTO))
					.isInstanceOf(BadCredentialsException.class);

			verifyNoInteractions(authentication);
		}

		@Test
		@Order(21)
		@DisplayName("should throw NullPointerException when request is null")
		void login_NullRequest_ThrowsException() {
			assertThatThrownBy(() -> userService.login(null))
					.isInstanceOf(NullPointerException.class);
		}

		@Test
		@Order(22)
		@DisplayName("should propagate ClassCastException if principal is not CustomUserDetails")
		void login_PrincipalNotCustomUserDetails_ThrowsClassCastException() {
			Object wrongPrincipal = "not-a-CustomUserDetails-object";
			given(authenticationManager.authenticate(any())).willReturn(authentication);
			given(authentication.getPrincipal()).willReturn(wrongPrincipal);

			assertThatThrownBy(() -> userService.login(loginRequestDTO))
					.isInstanceOf(ClassCastException.class);
		}

		@Test
		@Order(23)
		@DisplayName("should invoke no extra methods on authenticationManager")
		void login_NoExtraCallsOnAuthManager() {
			authenticate();
			userService.login(loginRequestDTO);
			verify(authenticationManager, times(1)).authenticate(any());
			verifyNoMoreInteractions(authenticationManager);
		}

		@Test
		@Order(24)
		@DisplayName("should invoke no extra methods on jwtService")
		void login_NoExtraCallsOnJwtService() {
			authenticate();
			userService.login(loginRequestDTO);
			verify(jwtService, times(1)).generateToken(any());
			verifyNoMoreInteractions(jwtService);
		}

		@Test
		@Order(25)
		@DisplayName("should call getUser() on CustomUserDetails exactly once")
		void login_CallsGetUserExactlyOnce() {
			authenticate();
			userService.login(loginRequestDTO);
			verify(customUserDetails, times(1)).getUser();
		}
	}


	@Nested
	@DisplayName("Update User API Testing")
	class UpdateUser
	{
		private static final long          USER_ID        = 1L;
		private static final String        ORIGINAL_EMAIL = "original@example.com";
		private static final String        UPDATED_EMAIL  = "updated@example.com";
		private static final Role          ORIGINAL_ROLE  = Role.ROLE_USER;
		private static final Role          UPDATED_ROLE   = Role.ROLE_ADMIN;
		private static final LocalDateTime CREATED_AT     = LocalDateTime.of(2024, 1, 1, 10, 0);
		private static final LocalDateTime UPDATED_AT     = LocalDateTime.of(2024, 6, 1, 10, 0);
		private User                 existingUser;
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
			given(userRepository.findActiveById(USER_ID))   // ← findActiveById, NOT findById
					.willReturn(Optional.of(existingUser));
			given(userRepository.save(any(User.class)))
					.willReturn(savedUser);
		}

		private void stubNotFound(long id) {
			given(userRepository.findActiveById(id))        // ← findActiveById, NOT findById
					.willReturn(Optional.empty());
		}

		@Test
		@Order(1)
		@DisplayName("should return a non-null UserResponseDTO on success")
		void updateUser_HappyPath_ReturnsNonNullDTO() {
			stubFoundAndSaved();

			UserResponseDTO result = userService.updateUser(USER_ID, validRequest);

			assertThat(result).isNotNull();
		}

		@Test
		@Order(2)
		@DisplayName("should return DTO with the id from the saved entity")
		void updateUser_ReturnsCorrectId() {
			stubFoundAndSaved();

			UserResponseDTO result = userService.updateUser(USER_ID, validRequest);

			assertThat(result.id()).isEqualTo(USER_ID);
		}

		@Test
		@Order(3)
		@DisplayName("should return DTO with the email from the saved entity")
		void updateUser_ReturnsEmailFromSavedEntity() {
			stubFoundAndSaved();

			UserResponseDTO result = userService.updateUser(USER_ID, validRequest);

			assertThat(result.email()).isEqualTo(UPDATED_EMAIL);
		}

		@Test
		@Order(4)
		@DisplayName("should return DTO with the role from the saved entity")
		void updateUser_ReturnsRoleFromSavedEntity() {
			stubFoundAndSaved();

			UserResponseDTO result = userService.updateUser(USER_ID, validRequest);

			assertThat(result.role()).isEqualTo(UPDATED_ROLE);
		}

		@Test
		@Order(5)
		@DisplayName("should return DTO with createdAt from the saved entity")
		void updateUser_ReturnsCreatedAt() {
			stubFoundAndSaved();

			UserResponseDTO result = userService.updateUser(USER_ID, validRequest);

			assertThat(result.createdAt()).isEqualTo(CREATED_AT);
		}

		@Test
		@Order(6)
		@DisplayName("should return DTO with updatedAt from the saved entity")
		void updateUser_ReturnsUpdatedAt() {
			stubFoundAndSaved();

			UserResponseDTO result = userService.updateUser(USER_ID, validRequest);

			assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
		}

		@Test
		@Order(7)
		@DisplayName("should call userRepository.findActiveById() exactly once")
		void updateUser_CallsFindActiveByIdExactlyOnce() {
			stubFoundAndSaved();

			userService.updateUser(USER_ID, validRequest);

			verify(userRepository, times(1)).findActiveById(USER_ID);
		}

		@Test
		@Order(8)
		@DisplayName("should call userRepository.save() exactly once")
		void updateUser_CallsSaveExactlyOnce() {
			stubFoundAndSaved();

			userService.updateUser(USER_ID, validRequest);

			verify(userRepository, times(1)).save(any(User.class));
		}

		@Test
		@Order(9)
		@DisplayName("should invoke no extra repository methods beyond findActiveById + save")
		void updateUser_NoExtraRepositoryInteractions() {
			stubFoundAndSaved();

			userService.updateUser(USER_ID, validRequest);

			verify(userRepository, times(1)).findActiveById(USER_ID);
			verify(userRepository, times(1)).save(any(User.class));
			verifyNoMoreInteractions(userRepository);
		}

		@Test
		@Order(10)
		@DisplayName("should never interact with passwordEncoder, authenticationManager or jwtService")
		void updateUser_NoInteractionsWithOtherDependencies() {
			stubFoundAndSaved();

			userService.updateUser(USER_ID, validRequest);

			verifyNoInteractions(passwordEncoder);
			verifyNoInteractions(authenticationManager);
			verifyNoInteractions(jwtService);
		}

		@Test
		@Order(11)
		@DisplayName("should pass a non-null User to save()")
		void updateUser_SaveReceivesNonNullUser() {
			stubFoundAndSaved();

			userService.updateUser(USER_ID, validRequest);

			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userRepository).save(captor.capture());
			assertThat(captor.getValue()).isNotNull();
		}

		@Test
		@Order(12)
		@DisplayName("should pass user with the correct id to save()")
		void updateUser_SavedUserHasCorrectId() {
			stubFoundAndSaved();

			userService.updateUser(USER_ID, validRequest);

			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userRepository).save(captor.capture());
			assertThat(captor.getValue().getId()).isEqualTo(USER_ID);
		}

		@Test
		@Order(13)
		@DisplayName("should pass user with isDeleted = false to save()")
		void updateUser_SavedUserIsNotDeleted() {
			stubFoundAndSaved();

			userService.updateUser(USER_ID, validRequest);

			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userRepository).save(captor.capture());
			assertThat(captor.getValue().isDeleted()).isFalse();
		}

		@Test
		@Order(14)
		@DisplayName("should pass the same entity returned by findActiveById to save()")
		void updateUser_PassesFetchedEntityToSave() {
			stubFoundAndSaved();

			userService.updateUser(USER_ID, validRequest);

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

			assertThatThrownBy(() -> userService.updateUser(USER_ID, validRequest))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@Order(16)
		@DisplayName("should include the id in the exception message")
		void updateUser_UserNotFound_ExceptionMessageContainsId() {
			stubNotFound(USER_ID);

			assertThatThrownBy(() -> userService.updateUser(USER_ID, validRequest))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining(String.valueOf(USER_ID));
		}

		@Test
		@Order(17)
		@DisplayName("should never call save() when user is not found")
		void updateUser_UserNotFound_SaveNeverCalled() {
			stubNotFound(USER_ID);

			assertThatThrownBy(() -> userService.updateUser(USER_ID, validRequest))
					.isInstanceOf(ResourceNotFoundException.class);

			verify(userRepository, never()).save(any());
		}

		@ParameterizedTest(name = "non-existent id = {0}")
		@Order(18)
		@ValueSource(longs = {99L, 999L, Long.MAX_VALUE})
		@DisplayName("should throw ResourceNotFoundException for any non-existent id")
		void updateUser_NonExistentIds_ThrowsResourceNotFoundException(long nonExistentId) {
			given(userRepository.findActiveById(nonExistentId)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.updateUser(nonExistentId, validRequest))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@Order(19)
		@DisplayName("should throw ResourceNotFoundException when id = 0")
		void updateUser_ZeroId_ThrowsResourceNotFoundException() {
			given(userRepository.findActiveById(0L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.updateUser(0L, validRequest))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@Order(20)
		@DisplayName("should throw ResourceNotFoundException when id is negative")
		void updateUser_NegativeId_ThrowsResourceNotFoundException() {
			given(userRepository.findActiveById(-1L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.updateUser(-1L, validRequest))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Order(21)
		@ParameterizedTest(name = "valid id = {0}")
		@ValueSource(longs = {1L, 50L, 100L, Long.MAX_VALUE})
		@DisplayName("should pass the exact id to findActiveById")
		void updateUser_PassesCorrectIdToFindActiveById(long id) {
			User user  = User.builder().id(id).email(ORIGINAL_EMAIL).role(ORIGINAL_ROLE)
					.isDeleted(false).createdAt(CREATED_AT).updatedAt(CREATED_AT)
					.createdById(0L).updatedById(0L).build();
			User saved = User.builder().id(id).email(UPDATED_EMAIL).role(UPDATED_ROLE)
					.isDeleted(false).createdAt(CREATED_AT).updatedAt(UPDATED_AT)
					.createdById(0L).updatedById(id).build();

			given(userRepository.findActiveById(id)).willReturn(Optional.of(user));
			given(userRepository.save(any(User.class))).willReturn(saved);

			userService.updateUser(id, validRequest);

			verify(userRepository).findActiveById(id);
		}

		@Test
		@Order(22)
		@DisplayName("should propagate DataIntegrityViolationException from save()")
		void updateUser_SaveThrowsDataIntegrity_Propagates() {
			given(userRepository.findActiveById(USER_ID)).willReturn(Optional.of(existingUser));
			given(userRepository.save(any(User.class)))
					.willThrow(new DataIntegrityViolationException("Duplicate email"));

			assertThatThrownBy(() -> userService.updateUser(USER_ID, validRequest))
					.isInstanceOf(DataIntegrityViolationException.class)
					.hasMessageContaining("Duplicate email");
		}

		@Test
		@Order(23)
		@DisplayName("should propagate OptimisticLockingFailureException from save()")
		void updateUser_SaveThrowsOptimisticLocking_Propagates() {
			given(userRepository.findActiveById(USER_ID)).willReturn(Optional.of(existingUser));
			given(userRepository.save(any(User.class)))
					.willThrow(new OptimisticLockingFailureException("Version conflict"));

			assertThatThrownBy(() -> userService.updateUser(USER_ID, validRequest))
					.isInstanceOf(OptimisticLockingFailureException.class);
		}

		@Test
		@Order(24)
		@DisplayName("should propagate RuntimeException from save()")
		void updateUser_SaveThrowsRuntimeException_Propagates() {
			given(userRepository.findActiveById(USER_ID)).willReturn(Optional.of(existingUser));
			given(userRepository.save(any(User.class)))
					.willThrow(new RuntimeException("Unexpected DB error"));

			assertThatThrownBy(() -> userService.updateUser(USER_ID, validRequest))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Unexpected DB error");
		}

		@Test
		@Order(25)
		@DisplayName("should propagate RuntimeException from findActiveById and never call save()")
		void updateUser_FindActiveByIdThrows_SaveNeverCalled() {
			given(userRepository.findActiveById(USER_ID))
					.willThrow(new RuntimeException("DB connection lost"));

			assertThatThrownBy(() -> userService.updateUser(USER_ID, validRequest))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("DB connection lost");

			verify(userRepository, never()).save(any());
		}

		@Test
		@Order(26)
		@DisplayName("should map all fields of the saved entity to the DTO")
		void updateUser_MapsAllFieldsFromSavedEntity() {
			stubFoundAndSaved();

			UserResponseDTO result = userService.updateUser(USER_ID, validRequest);

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

			UserResponseDTO result = userService.updateUser(USER_ID, validRequest);

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
					.id(USER_ID).email(UPDATED_EMAIL).role(UPDATED_ROLE)
					.isDeleted(false).createdAt(CREATED_AT).updatedAt(UPDATED_AT)
					.createdById(0L).updatedById(USER_ID)
					.build();

			given(userRepository.findActiveById(USER_ID)).willReturn(Optional.of(existingUser));
			given(userRepository.save(any(User.class))).willReturn(dbEnriched);

			UserResponseDTO result = userService.updateUser(USER_ID, validRequest);

			assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
			assertThat(result.updatedById()).isEqualTo(USER_ID);
		}

		@Test
		@Order(29)
		@DisplayName("should call findActiveById BEFORE save()")
		void updateUser_FetchesBeforeSaving() {
			stubFoundAndSaved();

			userService.updateUser(USER_ID, validRequest);

			var inOrder = inOrder(userRepository);
			inOrder.verify(userRepository).findActiveById(USER_ID);
			inOrder.verify(userRepository).save(any(User.class));
		}
	}

	@Nested
	@DisplayName("Get User By ID API Testing")
	class GetUserById
	{
		private static final long          USER_ID    = 1L;
		private static final String        EMAIL      = "john.doe@example.com";
		private static final Role          ROLE       = Role.ROLE_USER;
		private static final LocalDateTime CREATED_AT = LocalDateTime.of(2024, 1, 1, 10, 0);
		private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2024, 6, 1, 12, 0);
		private static final long          CREATED_BY = 0L;
		private static final long          UPDATED_BY = 2L;
		private User existingUser;

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
		}
		private void stubFound() {
			given(userRepository.findActiveById(USER_ID))  // ← findActiveById, NOT findById
					.willReturn(Optional.of(existingUser));
		}

		private void stubNotFound(long id) {
			given(userRepository.findActiveById(id))
					.willReturn(Optional.empty());
		}

		@Test
		@DisplayName("should return a non-null UserResponseDTO")
		void getUserById_HappyPath_ReturnsNonNullDTO() {
			stubFound();

			UserResponseDTO result = userService.getUserById(USER_ID);

			assertThat(result).isNotNull();
		}

		@Test
		@DisplayName("should return DTO with the correct id")
		void getUserById_ReturnsCorrectId() {
			stubFound();

			UserResponseDTO result = userService.getUserById(USER_ID);

			assertThat(result.id()).isEqualTo(USER_ID);
		}

		@Test
		@DisplayName("should return DTO with the correct email")
		void getUserById_ReturnsCorrectEmail() {
			stubFound();

			UserResponseDTO result = userService.getUserById(USER_ID);

			assertThat(result.email()).isEqualTo(EMAIL);
		}

		@Test
		@DisplayName("should return DTO with the correct role")
		void getUserById_ReturnsCorrectRole() {
			stubFound();

			UserResponseDTO result = userService.getUserById(USER_ID);

			assertThat(result.role()).isEqualTo(ROLE);
		}

		@Test
		@DisplayName("should return DTO with the correct createdAt")
		void getUserById_ReturnsCorrectCreatedAt() {
			stubFound();

			UserResponseDTO result = userService.getUserById(USER_ID);

			assertThat(result.createdAt()).isEqualTo(CREATED_AT);
		}

		@Test
		@DisplayName("should return DTO with the correct updatedAt")
		void getUserById_ReturnsCorrectUpdatedAt() {
			stubFound();

			UserResponseDTO result = userService.getUserById(USER_ID);

			assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
		}

		@Test
		@DisplayName("should return DTO with the correct createdById")
		void getUserById_ReturnsCorrectCreatedById() {
			stubFound();

			UserResponseDTO result = userService.getUserById(USER_ID);

			assertThat(result.createdById()).isEqualTo(CREATED_BY);
		}

		@Test
		@DisplayName("should return DTO with the correct updatedById")
		void getUserById_ReturnsCorrectUpdatedById() {
			stubFound();

			UserResponseDTO result = userService.getUserById(USER_ID);

			assertThat(result.updatedById()).isEqualTo(UPDATED_BY);
		}

		@Test
		@DisplayName("should call findActiveById() exactly once with the given id")
		void getUserById_CallsFindActiveByIdExactlyOnce() {
			stubFound();

			userService.getUserById(USER_ID);

			verify(userRepository, times(1)).findActiveById(USER_ID);
		}

		@Test
		@DisplayName("should invoke no extra repository methods beyond findActiveById")
		void getUserById_NoExtraRepositoryInteractions() {
			stubFound();

			userService.getUserById(USER_ID);

			verify(userRepository, times(1)).findActiveById(USER_ID);
			verifyNoMoreInteractions(userRepository);
		}

		@Test
		@DisplayName("should never interact with passwordEncoder, authenticationManager or jwtService")
		void getUserById_NoInteractionsWithOtherDependencies() {
			stubFound();

			userService.getUserById(USER_ID);

			verifyNoInteractions(passwordEncoder);
			verifyNoInteractions(authenticationManager);
			verifyNoInteractions(jwtService);
		}

		@Test
		@DisplayName("should never call save() or delete() on the repository")
		void getUserById_NeverMutatesRepository() {
			stubFound();

			userService.getUserById(USER_ID);

			verify(userRepository, never()).save(any());

		}

		@ParameterizedTest(name = "non-existent id = {0}")
		@ValueSource(longs = {99L, 999L, Long.MAX_VALUE})
		@DisplayName("should throw ResourceNotFoundException for any non-existent id")
		void getUserById_NonExistentIds_ThrowsResourceNotFoundException(long nonExistentId) {
			given(userRepository.findActiveById(nonExistentId)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.getUserById(nonExistentId))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@ParameterizedTest(name = "valid id = {0}")
		@ValueSource(longs = {1L, 50L, 100L, Long.MAX_VALUE})
		@DisplayName("should pass the exact id to findActiveById")
		void getUserById_PassesCorrectIdToRepository(long id) {
			User user = User.builder()
					.id(id).email(EMAIL).role(ROLE).isDeleted(false)
					.createdAt(CREATED_AT).updatedAt(UPDATED_AT)
					.createdById(CREATED_BY).updatedById(UPDATED_BY)
					.build();

			given(userRepository.findActiveById(id)).willReturn(Optional.of(user));

			userService.getUserById(id);

			verify(userRepository).findActiveById(id);
		}

		@Test
		@DisplayName("should throw ResourceNotFoundException when id = 0")
		void getUserById_ZeroId_ThrowsResourceNotFoundException() {
			given(userRepository.findActiveById(0L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.getUserById(0L))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@DisplayName("should throw ResourceNotFoundException when id is negative")
		void getUserById_NegativeId_ThrowsResourceNotFoundException() {
			given(userRepository.findActiveById(-1L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.getUserById(-1L))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@DisplayName("should propagate RuntimeException thrown by findActiveById")
		void getUserById_FindActiveByIdThrows_PropagatesException() {
			given(userRepository.findActiveById(USER_ID))
					.willThrow(new RuntimeException("DB connection lost"));

			assertThatThrownBy(() -> userService.getUserById(USER_ID))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("DB connection lost");
		}

		@Test
		@DisplayName("should never call save() or delete() when findActiveById throws")
		void getUserById_FindActiveByIdThrows_NeverMutatesRepository() {
			given(userRepository.findActiveById(USER_ID))
					.willThrow(new RuntimeException("DB connection lost"));

			assertThatThrownBy(() -> userService.getUserById(USER_ID))
					.isInstanceOf(RuntimeException.class);

			verify(userRepository, never()).save(any());

		}

	}

	@Nested
	@DisplayName("Change Password API Testing")
	class ChangePassword
	{
		private static final long          USER_ID        = 1L;
		private static final String        LOGGED_IN_EMAIL = "john.doe@example.com";
		private static final String        OLD_PASSWORD    = "OldPass@123";
		private static final String        NEW_PASSWORD    = "NewPass@456";
		private static final String        ENCODED_OLD     = "$2a$10$encodedOldHash";
		private static final String        ENCODED_NEW     = "$2a$10$encodedNewHash";
		private static final Role          ROLE            = Role.ROLE_USER;
		private static final LocalDateTime CREATED_AT      = LocalDateTime.of(2024, 1, 1, 10, 0);
		private static final LocalDateTime UPDATED_AT      = LocalDateTime.of(2024, 6, 1, 12, 0);

		// ─── Fixtures ─────────────────────────────────────────────────────────────

		private User                    existingUser;
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
					() -> userService.changePassword(validRequest)
			);
		}

		@Test
		@DisplayName("should call findByEmail() with the logged-in user's email")
		void changePassword_CallsFindByEmailWithLoggedInEmail() {
			stubFullHappyPath();

			userService.changePassword(validRequest);

			verify(userRepository, times(1)).findByEmail(LOGGED_IN_EMAIL);
		}

		@Test
		@DisplayName("should call passwordEncoder.matches() with the old password and stored hash")
		void changePassword_CallsPasswordMatchesWithCorrectArgs() {
			stubFullHappyPath();

			userService.changePassword(validRequest);

			verify(passwordEncoder, times(1)).matches(OLD_PASSWORD, ENCODED_OLD);
		}

		@Test
		@DisplayName("should call passwordEncoder.encode() with the new password")
		void changePassword_CallsEncodeWithNewPassword() {
			stubFullHappyPath();

			userService.changePassword(validRequest);

			verify(passwordEncoder, times(1)).encode(NEW_PASSWORD);
		}

		@Test
		@DisplayName("should call userRepository.save() exactly once")
		void changePassword_CallsSaveExactlyOnce() {
			stubFullHappyPath();

			userService.changePassword(validRequest);

			verify(userRepository, times(1)).save(any(User.class));
		}

		@Test
		@DisplayName("should update the user's password to the encoded new password before saving")
		void changePassword_SetsEncodedNewPasswordOnUserBeforeSave() {
			stubFullHappyPath();

			userService.changePassword(validRequest);

			// After changePassword(), the user entity's password must be the encoded new one
			assertThat(existingUser.getPassword())
					.as("User password must be updated to the encoded new password")
					.isEqualTo(ENCODED_NEW);
		}

		@Test
		@DisplayName("should save the user with the new encoded password")
		void changePassword_SavesUserWithNewEncodedPassword() {
			stubFullHappyPath();

			userService.changePassword(validRequest);

			verify(userRepository).save(argThat(u ->
					u.getPassword().equals(ENCODED_NEW)
			));
		}

		@Test
		@DisplayName("should invoke no extra repository methods beyond findByEmail + save")
		void changePassword_NoExtraRepositoryInteractions() {
			stubFullHappyPath();

			userService.changePassword(validRequest);

			verify(userRepository, times(1)).findByEmail(LOGGED_IN_EMAIL);
			verify(userRepository, times(1)).save(any(User.class));
			verifyNoMoreInteractions(userRepository);
		}

		@Test
		@DisplayName("should never interact with authenticationManager or jwtService")
		void changePassword_NoInteractionsWithUnusedDependencies() {
			stubFullHappyPath();

			userService.changePassword(validRequest);

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

			assertThatThrownBy(() -> userService.changePassword(validRequest))
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

			assertThatThrownBy(() -> userService.changePassword(validRequest))
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

			assertThatThrownBy(() -> userService.changePassword(validRequest))
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

			assertThatThrownBy(() -> userService.changePassword(validRequest))
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

			assertThatThrownBy(() -> userService.changePassword(validRequest))
					.isInstanceOf(BadCredentialsException.class);

			verify(userRepository, never()).save(any());
		}

		@Test
		@DisplayName("should call SecurityContextHolder.getContext() to resolve the logged-in user")
		void changePassword_CallsSecurityContextHolder() {
			stubFullHappyPath();

			userService.changePassword(validRequest);

			mockedSecurityContextHolder.verify(SecurityContextHolder::getContext, atLeastOnce());
		}

		@Test
		@DisplayName("should throw ResourceNotFoundException when no user found for logged-in email")
		void changePassword_UserNotFound_ThrowsResourceNotFoundException() {
			stubAuthenticatedUser();
			given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.changePassword(validRequest))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@DisplayName("should include the email in the ResourceNotFoundException message")
		void changePassword_UserNotFound_ExceptionContainsEmail() {
			stubAuthenticatedUser();
			given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.changePassword(validRequest))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining(LOGGED_IN_EMAIL);
		}

		@Test
		@DisplayName("should never call passwordEncoder.matches() when user not found")
		void changePassword_UserNotFound_PasswordMatchesNeverCalled() {
			stubAuthenticatedUser();
			given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.changePassword(validRequest))
					.isInstanceOf(ResourceNotFoundException.class);

			verify(passwordEncoder, never()).matches(any(), any());
		}

		@Test
		@DisplayName("should never call passwordEncoder.encode() when user not found")
		void changePassword_UserNotFound_EncodeNeverCalled() {
			stubAuthenticatedUser();
			given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.changePassword(validRequest))
					.isInstanceOf(ResourceNotFoundException.class);

			verify(passwordEncoder, never()).encode(any());
		}

		@Test
		@DisplayName("should never call save() when user not found")
		void changePassword_UserNotFound_SaveNeverCalled() {
			stubAuthenticatedUser();
			given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.changePassword(validRequest))
					.isInstanceOf(ResourceNotFoundException.class);

			verify(userRepository, never()).save(any());
		}

		@Test
		@DisplayName("should throw BadCredentialsException when old password does not match")
		void changePassword_WrongOldPassword_ThrowsBadCredentials() {
			stubAuthenticatedUser();
			given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.of(existingUser));
			given(passwordEncoder.matches(OLD_PASSWORD, ENCODED_OLD)).willReturn(false);

			assertThatThrownBy(() -> userService.changePassword(validRequest))
					.isInstanceOf(BadCredentialsException.class)
					.hasMessageContaining("Old password is incorrect");
		}

		@Test
		@DisplayName("should never call passwordEncoder.encode() when old password is wrong")
		void changePassword_WrongOldPassword_EncodeNeverCalled() {
			stubAuthenticatedUser();
			given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.of(existingUser));
			given(passwordEncoder.matches(OLD_PASSWORD, ENCODED_OLD)).willReturn(false);

			assertThatThrownBy(() -> userService.changePassword(validRequest))
					.isInstanceOf(BadCredentialsException.class);

			verify(passwordEncoder, never()).encode(any());
		}

		@Test
		@DisplayName("should never call save() when old password is wrong")
		void changePassword_WrongOldPassword_SaveNeverCalled() {
			stubAuthenticatedUser();
			given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.of(existingUser));
			given(passwordEncoder.matches(OLD_PASSWORD, ENCODED_OLD)).willReturn(false);

			assertThatThrownBy(() -> userService.changePassword(validRequest))
					.isInstanceOf(BadCredentialsException.class);

			verify(userRepository, never()).save(any());
		}

		@Test
		@DisplayName("should not update the user's password when old password is wrong")
		void changePassword_WrongOldPassword_PasswordNotUpdated() {
			stubAuthenticatedUser();
			given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.of(existingUser));
			given(passwordEncoder.matches(OLD_PASSWORD, ENCODED_OLD)).willReturn(false);

			assertThatThrownBy(() -> userService.changePassword(validRequest))
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

			userService.changePassword(validRequest);

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

			userService.changePassword(validRequest);

			var inOrder = inOrder(passwordEncoder);
			inOrder.verify(passwordEncoder).matches(OLD_PASSWORD, ENCODED_OLD);
			inOrder.verify(passwordEncoder).encode(NEW_PASSWORD);
		}

		@Test
		@DisplayName("should call encode() BEFORE save()")
		void changePassword_EncodesBeforeSaving() {
			stubFullHappyPath();

			userService.changePassword(validRequest);

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

			assertThatThrownBy(() -> userService.changePassword(validRequest))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("DB connection lost");
		}

		@Test
		@DisplayName("should never call save() when findByEmail throws")
		void changePassword_FindByEmailThrows_SaveNeverCalled() {
			stubAuthenticatedUser();
			given(userRepository.findByEmail(LOGGED_IN_EMAIL))
					.willThrow(new RuntimeException("DB connection lost"));

			assertThatThrownBy(() -> userService.changePassword(validRequest))
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

			assertThatThrownBy(() -> userService.changePassword(validRequest))
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

			assertThatThrownBy(() -> userService.changePassword(validRequest))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Encoding failed");

			verify(userRepository, never()).save(any());
		}

		@Test
		@DisplayName("should call matches() exactly once")
		void changePassword_CallsMatchesExactlyOnce() {
			stubFullHappyPath();

			userService.changePassword(validRequest);

			verify(passwordEncoder, times(1)).matches(any(), any());
		}

		@Test
		@DisplayName("should call encode() exactly once")
		void changePassword_CallsEncodeExactlyOnce() {
			stubFullHappyPath();

			userService.changePassword(validRequest);

			verify(passwordEncoder, times(1)).encode(any());
		}

		@Test
		@DisplayName("should never encode the old password")
		void changePassword_NeverEncodesOldPassword() {
			stubFullHappyPath();

			userService.changePassword(validRequest);

			verify(passwordEncoder, never()).encode(OLD_PASSWORD);
			verify(passwordEncoder, times(1)).encode(NEW_PASSWORD);
		}

		@Test
		@DisplayName("should never call matches() with the new password")
		void changePassword_NeverMatchesNewPassword() {
			stubFullHappyPath();

			userService.changePassword(validRequest);

			verify(passwordEncoder, never()).matches(eq(NEW_PASSWORD), any());
		}

		@Test
		@DisplayName("should store the encoded new password — never plain text")
		void changePassword_StoresEncodedPassword_NeverPlainText() {
			stubFullHappyPath();

			userService.changePassword(validRequest);

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

			assertThatThrownBy(() -> userService.changePassword(null))
					.isInstanceOf(NullPointerException.class);
		}

		@Test
		@DisplayName("should throw when old password is null (matches receives null)")
		void changePassword_NullOldPassword_ThrowsException() {
			ChangePasswordRequestDTO nullOldReq = new ChangePasswordRequestDTO(null, NEW_PASSWORD);

			stubAuthenticatedUser();
			given(userRepository.findByEmail(LOGGED_IN_EMAIL)).willReturn(Optional.of(existingUser));
			given(passwordEncoder.matches(null, ENCODED_OLD)).willReturn(false);

			assertThatThrownBy(() -> userService.changePassword(nullOldReq))
					.isInstanceOf(BadCredentialsException.class)
					.hasMessageContaining("Old password is incorrect");
		}
	}

	@Nested
	@DisplayName("Delete User API Testing")
	class DeleteUser
	{
		private static final long          USER_ID    = 1L;
		private static final String        EMAIL      = "john.doe@example.com";
		private static final Role          ROLE       = Role.ROLE_USER;
		private static final LocalDateTime CREATED_AT = LocalDateTime.of(2024, 1, 1, 10, 0);
		private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2024, 6, 1, 12, 0);

		// ─── Fixture ──────────────────────────────────────────────────────────────

		private User existingUser;
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
		 * getUserOrThrow() calls findActiveById() — NOT findById().
		 * Stubbing the wrong method returns Optional.empty() by default,
		 * firing ResourceNotFoundException before delete() is ever reached.
		 *
		 * NOTE: @SQLDelete on the User entity means userRepository.delete()
		 * triggers an UPDATE (soft delete) at the DB level, not a real DELETE.
		 * At the unit-test level we just verify delete() is called with the
		 * correct entity — the SQL rewrite is a JPA/Hibernate concern.
		 */
		private void stubFound() {
			given(userRepository.findActiveById(USER_ID))   // ← findActiveById, NOT findById
					.willReturn(Optional.of(existingUser));
		}

		private void stubNotFound(long id) {
			given(userRepository.findActiveById(id))
					.willReturn(Optional.empty());
		}

		@Test
		@DisplayName("should complete without throwing any exception")
		void softDeleteUser_HappyPath_NoExceptionThrown() {
			stubFound();

			assertThatNoException().isThrownBy(
					() -> userService.softDeleteUser(USER_ID)
			);
		}

		@Test
		@DisplayName("should return void (null) — method has no return value")
		void softDeleteUser_ReturnsVoid() {
			stubFound();

			// Just asserting the call completes — void methods have no return to check
			userService.softDeleteUser(USER_ID);

			// Reaching here means the method returned normally
			assertThat(true).isTrue();
		}

		@Test
		@DisplayName("should call findActiveById() exactly once with the given id")
		void softDeleteUser_CallsFindActiveByIdExactlyOnce() {
			stubFound();

			userService.softDeleteUser(USER_ID);

			verify(userRepository, times(1)).findActiveById(USER_ID);
		}

		@Test
		@DisplayName("should call delete() exactly once")
		void softDeleteUser_CallsDeleteExactlyOnce() {
			stubFound();

			userService.softDeleteUser(USER_ID);

			verify(userRepository, times(1)).delete(any(User.class));
		}

		@Test
		@DisplayName("should invoke no extra repository methods beyond findActiveById + delete")
		void softDeleteUser_NoExtraRepositoryInteractions() {
			stubFound();

			userService.softDeleteUser(USER_ID);

			verify(userRepository, times(1)).findActiveById(USER_ID);
			verify(userRepository, times(1)).delete(any(User.class));
			verifyNoMoreInteractions(userRepository);
		}

		@Test
		@DisplayName("should never call save() on the repository")
		void softDeleteUser_NeverCallsSave() {
			stubFound();

			userService.softDeleteUser(USER_ID);

			verify(userRepository, never()).save(any());
		}

		@Test
		@DisplayName("should never interact with passwordEncoder, authenticationManager or jwtService")
		void softDeleteUser_NoInteractionsWithOtherDependencies() {
			stubFound();

			userService.softDeleteUser(USER_ID);

			verifyNoInteractions(passwordEncoder);
			verifyNoInteractions(authenticationManager);
			verifyNoInteractions(jwtService);
		}

		@Test
		@DisplayName("should pass a non-null User to delete()")
		void softDeleteUser_DeleteReceivesNonNullUser() {
			stubFound();

			userService.softDeleteUser(USER_ID);

			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userRepository).delete(captor.capture());
			assertThat(captor.getValue()).isNotNull();
		}

		@Test
		@DisplayName("should pass the exact entity returned by findActiveById to delete()")
		void softDeleteUser_PassesFetchedEntityToDelete() {
			stubFound();

			userService.softDeleteUser(USER_ID);

			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userRepository).delete(captor.capture());

			// The entity passed to delete() must be the one fetched by getUserOrThrow
			assertThat(captor.getValue().getId()).isEqualTo(USER_ID);
			assertThat(captor.getValue().getEmail()).isEqualTo(EMAIL);
		}

		@Test
		@DisplayName("should pass entity with correct id to delete()")
		void softDeleteUser_DeletedEntityHasCorrectId() {
			stubFound();

			userService.softDeleteUser(USER_ID);

			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userRepository).delete(captor.capture());
			assertThat(captor.getValue().getId()).isEqualTo(USER_ID);
		}

		@Test
		@DisplayName("should pass entity with correct email to delete()")
		void softDeleteUser_DeletedEntityHasCorrectEmail() {
			stubFound();

			userService.softDeleteUser(USER_ID);

			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userRepository).delete(captor.capture());
			assertThat(captor.getValue().getEmail()).isEqualTo(EMAIL);
		}

		@Test
		@DisplayName("should pass entity with correct role to delete()")
		void softDeleteUser_DeletedEntityHasCorrectRole() {
			stubFound();

			userService.softDeleteUser(USER_ID);

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
			// This test confirms we pass the raw fetched entity directly to delete().
			stubFound();

			userService.softDeleteUser(USER_ID);

			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userRepository).delete(captor.capture());
			assertThat(captor.getValue().isDeleted()).isFalse();
		}

		@Test
		@DisplayName("should pass entity with null deletedTimestamp — set by @SQLDelete at DB level")
		void softDeleteUser_EntityPassedToDeleteHasNullDeletedTimestamp() {
			// deletedTimestamp is set by the DB via @SQLDelete — not by this service.
			stubFound();

			userService.softDeleteUser(USER_ID);

			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userRepository).delete(captor.capture());
			assertThat(captor.getValue().getDeletedTimestamp()).isNull();
		}

		@Test
		@DisplayName("should throw ResourceNotFoundException when user does not exist")
		void softDeleteUser_UserNotFound_ThrowsResourceNotFoundException() {
			stubNotFound(USER_ID);

			assertThatThrownBy(() -> userService.softDeleteUser(USER_ID))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@DisplayName("should include the searched id in the exception message")
		void softDeleteUser_UserNotFound_ExceptionContainsId() {
			stubNotFound(USER_ID);

			assertThatThrownBy(() -> userService.softDeleteUser(USER_ID))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining(String.valueOf(USER_ID));
		}

		@Test
		@DisplayName("should never call delete() when user is not found")
		void softDeleteUser_UserNotFound_DeleteNeverCalled() {
			stubNotFound(USER_ID);

			assertThatThrownBy(() -> userService.softDeleteUser(USER_ID))
					.isInstanceOf(ResourceNotFoundException.class);

			verify(userRepository, never()).delete(any(User.class));
		}

		@Test
		@DisplayName("should never call save() when user is not found")
		void softDeleteUser_UserNotFound_SaveNeverCalled() {
			stubNotFound(USER_ID);

			assertThatThrownBy(() -> userService.softDeleteUser(USER_ID))
					.isInstanceOf(ResourceNotFoundException.class);

			verify(userRepository, never()).save(any());
		}

		@Test
		@DisplayName("should call findActiveById() exactly once even when user is not found")
		void softDeleteUser_UserNotFound_FindActiveByIdCalledOnce() {
			stubNotFound(USER_ID);

			assertThatThrownBy(() -> userService.softDeleteUser(USER_ID))
					.isInstanceOf(ResourceNotFoundException.class);

			verify(userRepository, times(1)).findActiveById(USER_ID);
		}

		@ParameterizedTest(name = "non-existent id = {0}")
		@ValueSource(longs = {99L, 999L, Long.MAX_VALUE})
		@DisplayName("should throw ResourceNotFoundException for any non-existent id")
		void softDeleteUser_NonExistentIds_ThrowsResourceNotFoundException(long nonExistentId) {
			given(userRepository.findActiveById(nonExistentId)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.softDeleteUser(nonExistentId))
					.isInstanceOf(ResourceNotFoundException.class);

			verify(userRepository, never()).delete(any(User.class));
		}

		@ParameterizedTest(name = "valid id = {0}")
		@ValueSource(longs = {1L, 50L, 100L, Long.MAX_VALUE})
		@DisplayName("should pass the exact id to findActiveById")
		void softDeleteUser_PassesCorrectIdToFindActiveById(long id) {
			User user = User.builder()
					.id(id).email(EMAIL).password("encoded").role(ROLE)
					.isDeleted(false).createdAt(CREATED_AT).updatedAt(UPDATED_AT)
					.createdById(0L).updatedById(0L)
					.build();

			given(userRepository.findActiveById(id)).willReturn(Optional.of(user));

			userService.softDeleteUser(id);

			verify(userRepository).findActiveById(id);
			verify(userRepository).delete(user);
		}

		@Test
		@DisplayName("should throw ResourceNotFoundException when id = 0")
		void softDeleteUser_ZeroId_ThrowsResourceNotFoundException() {
			given(userRepository.findActiveById(0L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.softDeleteUser(0L))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@DisplayName("should throw ResourceNotFoundException when id is negative")
		void softDeleteUser_NegativeId_ThrowsResourceNotFoundException() {
			given(userRepository.findActiveById(-1L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.softDeleteUser(-1L))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@DisplayName("should call findActiveById BEFORE delete()")
		void softDeleteUser_FetchesBeforeDeleting() {
			stubFound();

			userService.softDeleteUser(USER_ID);

			var inOrder = inOrder(userRepository);
			inOrder.verify(userRepository).findActiveById(USER_ID);
			inOrder.verify(userRepository).delete(any(User.class));
		}

		@Test
		@DisplayName("should propagate RuntimeException thrown by findActiveById")
		void softDeleteUser_FindActiveByIdThrows_PropagatesException() {
			given(userRepository.findActiveById(USER_ID))
					.willThrow(new RuntimeException("DB connection lost"));

			assertThatThrownBy(() -> userService.softDeleteUser(USER_ID))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("DB connection lost");
		}

		@Test
		@DisplayName("should never call delete() when findActiveById throws")
		void softDeleteUser_FindActiveByIdThrows_DeleteNeverCalled() {
			given(userRepository.findActiveById(USER_ID))
					.willThrow(new RuntimeException("DB connection lost"));

			assertThatThrownBy(() -> userService.softDeleteUser(USER_ID))
					.isInstanceOf(RuntimeException.class);

			verify(userRepository, never()).delete(any(User.class));
		}

		@Test
		@DisplayName("should propagate RuntimeException thrown by delete()")
		void softDeleteUser_DeleteThrows_PropagatesException() {
			// findActiveById must be stubbed first — otherwise ResourceNotFoundException
			// fires before delete() is ever reached
			stubFound();
			willThrow(new RuntimeException("Delete failed"))
					.given(userRepository).delete(any(User.class));

			assertThatThrownBy(() -> userService.softDeleteUser(USER_ID))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Delete failed");
		}

		@Test
		@DisplayName("should propagate DataIntegrityViolationException from delete()")
		void softDeleteUser_DeleteThrowsDataIntegrity_Propagates() {
			stubFound();
			willThrow(new org.springframework.dao.DataIntegrityViolationException("Constraint violation"))
					.given(userRepository).delete(any(User.class));

			assertThatThrownBy(() -> userService.softDeleteUser(USER_ID))
					.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		}

		@Test
		@DisplayName("should delegate to repository.delete() — @SQLDelete rewrites to UPDATE at DB level")
		void softDeleteUser_DelegatesDeleteToRepository() {
			// The service's job is only to call repository.delete(user).
			// The @SQLDelete annotation on User entity handles the actual SQL rewrite:
			//   UPDATE users SET um_is_deleted = true, um_deleted_timestamp = CURRENT_TIMESTAMP
			//   WHERE um_id = ?
			// This is transparent to the service layer and verified only in integration tests.
			stubFound();

			userService.softDeleteUser(USER_ID);

			verify(userRepository, times(1)).delete(existingUser);
		}

		@Test
		@DisplayName("should not manually set isDeleted or deletedTimestamp — that is @SQLDelete's job")
		void softDeleteUser_DoesNotManuallySetIsDeletedOrTimestamp() {
			stubFound();

			userService.softDeleteUser(USER_ID);

			// The entity handed to delete() must be unmodified — isDeleted and
			// deletedTimestamp are set by Hibernate's @SQLDelete, not the service
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
		@DisplayName("should call findActiveById (not findById) to exclude already-deleted users")
		void softDeleteUser_UsesActiveUserQuery_ExcludesAlreadyDeletedUsers() {
			// findActiveById returns only non-deleted users, preventing a double soft-delete
			stubFound();

			userService.softDeleteUser(USER_ID);

			verify(userRepository).findActiveById(USER_ID);
			verify(userRepository, never()).findById(any());
		}
	}

	@Nested
	@DisplayName("Search User API Testing")
	class SearchUser
	{
		private static final long USER_ID_1 = 1L;
		private static final long USER_ID_2 = 2L;
		private static final String EMAIL_1 = "alice@example.com";
		private static final String EMAIL_2 = "bob@example.com";
		private static final Role ROLE_USER = Role.ROLE_USER;
		private static final Role ROLE_ADMIN = Role.ROLE_ADMIN;
		private static final LocalDateTime CREATED_AT = LocalDateTime.of(2024, 1, 1, 10, 0);
		private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2024, 6, 1, 10, 0);

		private User user1;
		private User user2;
		private Pageable pageable;
		private UserSearchRequestDTO blankRequest;

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

			pageable = PageRequest.of(0, 10);

			blankRequest = new UserSearchRequestDTO(null, null, 1, 1,LocalDateTime.now(),LocalDateTime.now());
		}

		// ── helper ────────────────────────────────────────────────────────────────

		private Page<User> pageOf(User... users) {
			return new PageImpl<>(List.of(users), pageable, users.length);
		}

		private void stubSearch(UserSearchRequestDTO request, Page<User> page) {
			given(userRepository.findAll(any(Specification.class), eq(pageable)))
					.willReturn(page);
		}

		@Test
		@Order(1)
		@DisplayName("should return a non-null Page on success")
		void searchUsers_ReturnsNonNullPage() {
			stubSearch(blankRequest, pageOf(user1));

			Page<UserResponseDTO> result = userService.searchUsers(blankRequest, pageable);

			assertThat(result).isNotNull();
		}

		@Test
		@Order(2)
		@DisplayName("should return an empty page when repository returns no results")
		void searchUsers_EmptyResult_ReturnsEmptyPage() {
			given(userRepository.findAll(any(Specification.class), eq(pageable)))
					.willReturn(Page.empty(pageable));

			Page<UserResponseDTO> result = userService.searchUsers(blankRequest, pageable);

			assertThat(result.getTotalElements()).isZero();
			assertThat(result.getContent()).asList().isEmpty();
		}

		@Test
		@Order(3)
		@DisplayName("should return page with correct total element count")
		void searchUsers_ReturnsTotalElementCount() {
			stubSearch(blankRequest, pageOf(user1, user2));

			Page<UserResponseDTO> result = userService.searchUsers(blankRequest, pageable);

			assertThat(result.getTotalElements()).isEqualTo(2);
		}

		@Test
		@Order(4)
		@DisplayName("should map each user entity to a UserResponseDTO")
		void searchUsers_MapsEntitiesToDTOs() {
			stubSearch(blankRequest, pageOf(user1, user2));

			Page<UserResponseDTO> result = userService.searchUsers(blankRequest, pageable);

			assertThat(result.getContent()).asList().hasSize(2);
		}

		// ── DTO field mapping ─────────────────────────────────────────────────────

		@Test
		@Order(5)
		@DisplayName("should map id correctly for each DTO in the page")
		void searchUsers_MapsIdCorrectly() {
			stubSearch(blankRequest, pageOf(user1));

			Page<UserResponseDTO> result = userService.searchUsers(blankRequest, pageable);

			assertThat(result.getContent().get(0).id()).isEqualTo(USER_ID_1);
		}

		@Test
		@Order(6)
		@DisplayName("should map email correctly for each DTO in the page")
		void searchUsers_MapsEmailCorrectly() {
			stubSearch(blankRequest, pageOf(user1));

			Page<UserResponseDTO> result = userService.searchUsers(blankRequest, pageable);

			assertThat(result.getContent().get(0).email()).isEqualTo(EMAIL_1);
		}

		@Test
		@Order(7)
		@DisplayName("should map role correctly for each DTO in the page")
		void searchUsers_MapsRoleCorrectly() {
			stubSearch(blankRequest, pageOf(user1));

			Page<UserResponseDTO> result = userService.searchUsers(blankRequest, pageable);

			assertThat(result.getContent().get(0).role()).isEqualTo(ROLE_USER);
		}

		@Test
		@Order(8)
		@DisplayName("should map createdAt correctly for each DTO in the page")
		void searchUsers_MapsCreatedAtCorrectly() {
			stubSearch(blankRequest, pageOf(user1));

			Page<UserResponseDTO> result = userService.searchUsers(blankRequest, pageable);

			assertThat(result.getContent().get(0).createdAt()).isEqualTo(CREATED_AT);
		}

		@Test
		@Order(9)
		@DisplayName("should map updatedAt correctly for each DTO in the page")
		void searchUsers_MapsUpdatedAtCorrectly() {
			stubSearch(blankRequest, pageOf(user1));

			Page<UserResponseDTO> result = userService.searchUsers(blankRequest, pageable);

			assertThat(result.getContent().get(0).updatedAt()).isEqualTo(UPDATED_AT);
		}

		@Test
		@Order(10)
		@DisplayName("should map all fields of all entities to DTOs")
		void searchUsers_MapsAllFieldsForAllEntities() {
			stubSearch(blankRequest, pageOf(user1, user2));

			Page<UserResponseDTO> result = userService.searchUsers(blankRequest, pageable);
			List<UserResponseDTO> content = result.getContent();

			assertThat(content.get(0).id()).isEqualTo(USER_ID_1);
			assertThat(content.get(0).email()).isEqualTo(EMAIL_1);
			assertThat(content.get(0).role()).isEqualTo(ROLE_USER);

			assertThat(content.get(1).id()).isEqualTo(USER_ID_2);
			assertThat(content.get(1).email()).isEqualTo(EMAIL_2);
			assertThat(content.get(1).role()).isEqualTo(ROLE_ADMIN);
		}

		// ── Repository interaction ────────────────────────────────────────────────

		@Test
		@Order(11)
		@DisplayName("should call userRepository.findAll() with Specification and Pageable exactly once")
		void searchUsers_CallsFindAllExactlyOnce() {
			stubSearch(blankRequest, pageOf(user1));

			userService.searchUsers(blankRequest, pageable);

			verify(userRepository, times(1))
					.findAll(any(Specification.class), eq(pageable));
		}

		@Test
		@Order(12)
		@DisplayName("should invoke no extra repository methods beyond findAll()")
		void searchUsers_NoExtraRepositoryInteractions() {
			stubSearch(blankRequest, pageOf(user1));

			userService.searchUsers(blankRequest, pageable);

			verify(userRepository, times(1))
					.findAll(any(Specification.class), eq(pageable));
			verifyNoMoreInteractions(userRepository);
		}

		@Test
		@Order(13)
		@DisplayName("should pass the exact Pageable to repository")
		void searchUsers_PassesExactPageableToRepository() {
			Pageable customPageable = PageRequest.of(2, 5);
			given(userRepository.findAll(any(Specification.class), eq(customPageable)))
					.willReturn(new PageImpl<>(List.of(user1), customPageable, 1));

			userService.searchUsers(blankRequest, customPageable);

			verify(userRepository).findAll(any(Specification.class), eq(customPageable));
		}

		@Test
		@Order(14)
		@DisplayName("should never interact with passwordEncoder, authenticationManager or jwtService")
		void searchUsers_NoInteractionsWithOtherDependencies() {
			stubSearch(blankRequest, pageOf(user1));

			userService.searchUsers(blankRequest, pageable);

			verifyNoInteractions(passwordEncoder);
			verifyNoInteractions(authenticationManager);
			verifyNoInteractions(jwtService);
		}

		// ── Pagination metadata ───────────────────────────────────────────────────

		@Test
		@Order(15)
		@DisplayName("should preserve page number from repository result")
		void searchUsers_PreservesPageNumber() {
			Pageable page2 = PageRequest.of(1, 5);
			given(userRepository.findAll(any(Specification.class), eq(page2)))
					.willReturn(new PageImpl<>(List.of(user1), page2, 11));

			Page<UserResponseDTO> result = userService.searchUsers(blankRequest, page2);

			assertThat(result.getNumber()).isEqualTo(1);
		}

		@Test
		@Order(16)
		@DisplayName("should preserve page size from repository result")
		void searchUsers_PreservesPageSize() {
			Pageable size5 = PageRequest.of(0, 5);
			given(userRepository.findAll(any(Specification.class), eq(size5)))
					.willReturn(new PageImpl<>(List.of(user1, user2), size5, 2));

			Page<UserResponseDTO> result = userService.searchUsers(blankRequest, size5);

			assertThat(result.getSize()).isEqualTo(5);
		}

		@Test
		@Order(17)
		@DisplayName("should preserve total pages count from repository result")
		void searchUsers_PreservesTotalPages() {
			given(userRepository.findAll(any(Specification.class), eq(pageable)))
					.willReturn(new PageImpl<>(List.of(user1, user2), pageable, 25));

			Page<UserResponseDTO> result = userService.searchUsers(blankRequest, pageable);

			assertThat(result.getTotalPages()).isEqualTo(3); // ceil(25/10)
		}

		@Test
		@Order(18)
		@DisplayName("should return a single-element page when repository returns one user")
		void searchUsers_SingleResult_ReturnsOneElementPage() {
			stubSearch(blankRequest, pageOf(user1));

			Page<UserResponseDTO> result = userService.searchUsers(blankRequest, pageable);

			assertThat(result.getContent()).asList().hasSize(1);
		}

		// ── Filter-to-specification passthrough ───────────────────────────────────

		@Test
		@Order(19)
		@DisplayName("should pass a non-null Specification to repository")
		void searchUsers_PassesNonNullSpecificationToRepository() {
			stubSearch(blankRequest, pageOf(user1));

			userService.searchUsers(blankRequest, pageable);

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

			userService.searchUsers(allNull, pageable);

			verify(userRepository, times(1))
					.findAll(any(Specification.class), eq(pageable));
		}

		@Test
		@Order(21)
		@DisplayName("should still invoke findAll once when email filter is provided")
		void searchUsers_WithEmailFilter_CallsFindAllOnce() {
			UserSearchRequestDTO emailFilter =
					new UserSearchRequestDTO(EMAIL_1, null, 1, 1, null, null);
			stubSearch(emailFilter, pageOf(user1));

			userService.searchUsers(emailFilter, pageable);

			verify(userRepository, times(1))
					.findAll(any(Specification.class), eq(pageable));
		}

		@Test
		@Order(22)
		@DisplayName("should still invoke findAll once when role filter is provided")
		void searchUsers_WithRoleFilter_CallsFindAllOnce() {
			UserSearchRequestDTO roleFilter =
					new UserSearchRequestDTO(null, ROLE_USER, 1, 1, null, null);
			stubSearch(roleFilter, pageOf(user1));

			userService.searchUsers(roleFilter, pageable);

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
			stubSearch(dateFilter, pageOf(user1, user2));

			userService.searchUsers(dateFilter, pageable);

			verify(userRepository, times(1))
					.findAll(any(Specification.class), eq(pageable));
		}

		// ── Exception propagation ─────────────────────────────────────────────────

		@Test
		@Order(24)
		@DisplayName("should propagate RuntimeException thrown by repository")
		void searchUsers_RepositoryThrows_PropagatesException() {
			given(userRepository.findAll(any(Specification.class), eq(pageable)))
					.willThrow(new RuntimeException("DB unavailable"));

			assertThatThrownBy(() -> userService.searchUsers(blankRequest, pageable))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("DB unavailable");
		}

		@Test
		@Order(25)
		@DisplayName("should throw NullPointerException when request is null")
		void searchUsers_NullRequest_ThrowsNullPointerException() {
			assertThatThrownBy(() -> userService.searchUsers(null, pageable))
					.isInstanceOf(NullPointerException.class);
		}

		@Test
		@Order(26)
		@DisplayName("should throw NullPointerException when pageable is null")
		void searchUsers_NullPageable_ThrowsNullPointerException() {
			assertThatThrownBy(() -> userService.searchUsers(blankRequest, null))
					.isInstanceOf(NullPointerException.class);
		}

		// ── Role enum coverage ────────────────────────────────────────────────────

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

			given(userRepository.findAll(any(Specification.class), eq(pageable)))
					.willReturn(pageOf(userWithRole));

			Page<UserResponseDTO> result = userService.searchUsers(blankRequest, pageable);

			assertThat(result.getContent().get(0).role()).isEqualTo(role);
		}
	}

}