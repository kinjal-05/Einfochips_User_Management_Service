package userservice.services;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import userservice.dtos.UserLoginRequestDTO;
import userservice.dtos.UserLoginResponseDTO;
import userservice.enums.Role;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.security.CustomUserDetails;
import userservice.security.JwtService;
import userservice.services.impls.UserLoginServiceImpl;
import userservice.utility.MapToUserResponseDTO;

/**
 * Comprehensive unit test suite for
 * {@link UserLoginServiceImpl#login(UserLoginRequestDTO)}.
 *
 * <p>
 * This test class validates the login/authentication workflow in isolation
 * using Mockito and JUnit 5. It ensures correct authentication handling, JWT
 * generation, response construction, and robust error propagation.
 * </p>
 *
 * <h3>Test Coverage</h3>
 * <ul>
 * <li><b>Happy Path:</b> Verifies successful authentication and ensures a fully
 * populated {@link UserLoginResponseDTO} is returned.</li>
 *
 * <li><b>Authentication Flow:</b>
 * <ul>
 * <li>Ensures credentials are passed correctly to
 * {@link AuthenticationManager}</li>
 * <li>Validates {@link Authentication#getPrincipal()} is used to extract user
 * details</li>
 * <li>Confirms proper casting to {@link CustomUserDetails}</li>
 * </ul>
 * </li>
 *
 * <li><b>JWT Token Handling:</b>
 * <ul>
 * <li>Ensures token is generated using {@link JwtService}</li>
 * <li>Validates token is embedded correctly in the response DTO</li>
 * <li>Confirms token generation occurs only after successful
 * authentication</li>
 * </ul>
 * </li>
 *
 * <li><b>Response Validation:</b>
 * <ul>
 * <li>Verifies correctness of user ID, email, role, and message</li>
 * <li>Ensures response is never null</li>
 * </ul>
 * </li>
 *
 * <li><b>Interaction Verification:</b>
 * <ul>
 * <li>Ensures {@code authenticate()} is called exactly once</li>
 * <li>Ensures {@code generateToken()} is called exactly once</li>
 * <li>Validates correct order of operations (authenticate → principal →
 * token)</li>
 * <li>Confirms no unnecessary interactions with dependencies</li>
 * </ul>
 * </li>
 *
 * <li><b>Exception Handling:</b>
 * <ul>
 * <li>Propagates {@link BadCredentialsException} for invalid credentials</li>
 * <li>Handles {@link DisabledException} for disabled accounts</li>
 * <li>Handles {@link LockedException} for locked accounts</li>
 * <li>Propagates {@link ClassCastException} for invalid principal type</li>
 * </ul>
 * </li>
 *
 * <li><b>Failure Scenarios:</b>
 * <ul>
 * <li>Ensures JWT generation is skipped when authentication fails</li>
 * <li>Ensures principal extraction is skipped on failure</li>
 * </ul>
 * </li>
 *
 * <li><b>Edge Cases:</b>
 * <ul>
 * <li>Null request handling</li>
 * <li>Invalid principal type handling</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <h3>Testing Strategy</h3>
 * <ul>
 * <li>Uses {@link MockitoExtension} for mock initialization</li>
 * <li>Follows Arrange-Act-Assert pattern</li>
 * <li>Uses helper methods for reusable authentication stubbing</li>
 * <li>Captures arguments to validate correctness of authentication
 * requests</li>
 * </ul>
 *
 * <h3>Key Design Considerations</h3>
 * <ul>
 * <li>Ensures strict isolation of authentication logic</li>
 * <li>Validates secure handling of credentials and tokens</li>
 * <li>Prevents regression in login and security workflows</li>
 * <li>Maintains high readability and maintainability</li>
 * </ul>
 *
 * <p>
 * This test suite is designed to meet production-grade standards and ensure
 * reliability, security, and correctness of the login functionality.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - login()")
@ActiveProfiles("test")
class UserLoginServiceTest {
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
	// Service under test
	@InjectMocks
	private UserLoginServiceImpl loginUserService;

	private static final Long USER_ID = 10L;
	private static final String TEST_EMAIL = "john.doe@example.com";
	private static final String TEST_PASSWORD = "Secret@123";
	private static final String JWT_TOKEN = "eyJhbGciOiJIUzI1NiJ9.test.token";
	private static final Role TEST_ROLE = Role.ROLE_USER;

	private UserLoginRequestDTO loginRequestDTO;
	private User savedUser;

	/**
	 * Initializes login request and base user entity.
	 *
	 * Setup includes: - Login request DTO preparation - Mock user entity
	 * representing authenticated user
	 */
	@BeforeEach
	void setUp() {
		loginRequestDTO = new UserLoginRequestDTO(TEST_EMAIL, TEST_PASSWORD);

		savedUser = User.builder().id(USER_ID).email(TEST_EMAIL).role(TEST_ROLE).isDeleted(false).build();
	}

	/**
	 * Stubs full authentication flow including JWT generation.
	 *
	 * Flow: - AuthenticationManager authenticates credentials - Principal is
	 * resolved to CustomUserDetails - User entity is extracted from principal - JWT
	 * token is generated from user details
	 */
	private void authenticate() {
		given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.willReturn(authentication);
		given(authentication.getPrincipal()).willReturn(customUserDetails);
		given(customUserDetails.getUser()).willReturn(savedUser);
		given(jwtService.generateToken(customUserDetails)).willReturn(JWT_TOKEN);
	}

	/**
	 * Ensures full login response is correctly constructed.
	 */
	@Test
	@Order(1)
	@DisplayName("should return a fully populated LoginResponseDTO on success")
	void login_ReturnsFullDTO() {
		authenticate();
		UserLoginResponseDTO result = loginUserService.login(loginRequestDTO);
		assertThat(result).isNotNull();
		assertThat(result.id()).isEqualTo(USER_ID);
		assertThat(result.email()).isEqualTo(TEST_EMAIL);
		assertThat(result.role()).isEqualTo(TEST_ROLE);
		assertThat(result.token()).isEqualTo(JWT_TOKEN);
		assertThat(result.message()).isEqualTo("Login Successful");
	}

	/**
	 * Ensures JWT token is returned exactly as generated by JwtService.
	 */
	@Test
	@Order(2)
	@DisplayName("should return the JWT token generated by JwtService")
	void login_ReturnsTokenFromJwtService() {
		authenticate();
		UserLoginResponseDTO result = loginUserService.login(loginRequestDTO);
		assertThat(result.token()).isEqualTo(JWT_TOKEN);
	}

	/**
	 * Ensures user ID is correctly extracted from authenticated principal.
	 */
	@Test
	@Order(3)
	@DisplayName("should return the user id from the authenticated principal")
	void login_ReturnsCorrectUserId() {
		authenticate();
		UserLoginResponseDTO result = loginUserService.login(loginRequestDTO);
		assertThat(result.id()).isEqualTo(USER_ID);
	}

	/**
	 * Ensures email is correctly returned from authenticated user entity.
	 */
	@Test
	@Order(4)
	@DisplayName("should return the email from the authenticated User entity")
	void login_ReturnsCorrectEmail() {
		authenticate();
		UserLoginResponseDTO result = loginUserService.login(loginRequestDTO);
		assertThat(result.email()).isEqualTo(TEST_EMAIL);
	}

	/**
	 * Ensures role is correctly returned from authenticated user entity.
	 */
	@Test
	@Order(5)
	@DisplayName("should return the role from the authenticated User entity")
	void login_ReturnsCorrectRole() {
		authenticate();
		UserLoginResponseDTO result = loginUserService.login(loginRequestDTO);
		assertThat(result.role()).isEqualTo(TEST_ROLE);
	}

	/**
	 * Ensures service never returns a null response.
	 */
	@Test
	@Order(6)
	@DisplayName("should never return null")
	void login_NeverReturnsNull() {
		authenticate();
		UserLoginResponseDTO result = loginUserService.login(loginRequestDTO);
		assertThat(result).isNotNull();
	}

	/**
	 * Ensures correct credentials are passed to AuthenticationManager.
	 */
	@Test
	@Order(7)
	@DisplayName("should pass email and password from the request to authenticationManager")
	void login_PassesCorrectCredentialsToAuthManager() {
		authenticate();
		loginUserService.login(loginRequestDTO);
		ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor
				.forClass(UsernamePasswordAuthenticationToken.class);
		verify(authenticationManager).authenticate(captor.capture());
		UsernamePasswordAuthenticationToken captured = captor.getValue();
		assertThat(captured.getPrincipal()).isEqualTo(TEST_EMAIL);
		assertThat(captured.getCredentials()).isEqualTo(TEST_PASSWORD);
	}

	/**
	 * Ensures authentication is invoked exactly once per login attempt.
	 */
	@Test
	@Order(8)
	@DisplayName("should call authenticationManager.authenticate() exactly once")
	void login_CallsAuthManagerExactlyOnce() {
		authenticate();
		loginUserService.login(loginRequestDTO);
		verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
	}

	/**
	 * Ensures authentication principal is resolved correctly.
	 */
	@Test
	@Order(9)
	@DisplayName("should cast authentication principal to CustomUserDetails")
	void login_CastsPrincipalToCustomUserDetails() {
		authenticate();
		loginUserService.login(loginRequestDTO);
		// getPrincipal() must be called to obtain CustomUserDetails
		verify(authentication, times(1)).getPrincipal();
	}

	/**
	 * Ensures JWT generation is strictly delegated to JwtService.
	 */
	@Test
	@Order(10)
	@DisplayName("should call jwtService.generateToken() exactly once with CustomUserDetails")
	void login_CallsJwtServiceExactlyOnce() {
		authenticate();
		loginUserService.login(loginRequestDTO);
		verify(jwtService, times(1)).generateToken(customUserDetails);
		verifyNoMoreInteractions(jwtService);
	}

	/**
	 * Ensures JwtService receives CustomUserDetails and not raw User entity.
	 */
	@Test
	@Order(11)
	@DisplayName("should pass the principal (CustomUserDetails) to jwtService, not the raw user")
	void login_PassesCustomUserDetailsTOJwtService() {
		authenticate();
		loginUserService.login(loginRequestDTO);
		ArgumentCaptor<CustomUserDetails> captor = ArgumentCaptor.forClass(CustomUserDetails.class);
		verify(jwtService).generateToken(captor.capture());
		assertThat(captor.getValue()).isSameAs(customUserDetails);
	}

	/**
	 * Ensures JWT token returned in response matches JwtService output exactly.
	 */
	@Test
	@Order(12)
	@DisplayName("should embed the JWT from jwtService directly in the response DTO")
	void login_TokenInDTOMatchesJwtServiceOutput() {
		String customToken = "custom.jwt.token.xyz";
		given(authenticationManager.authenticate(any())).willReturn(authentication);
		given(authentication.getPrincipal()).willReturn(customUserDetails);
		given(customUserDetails.getUser()).willReturn(savedUser);
		given(jwtService.generateToken(customUserDetails)).willReturn(customToken);

		UserLoginResponseDTO result = loginUserService.login(loginRequestDTO);

		assertThat(result.token()).isEqualTo(customToken);
	}

	/**
	 * Ensures authentication is executed before JWT token generation.
	 */
	@Test
	@Order(13)
	@DisplayName("should authenticate BEFORE generating a JWT token")
	void login_AuthenticatesBeforeGeneratingToken() {
		authenticate();
		loginUserService.login(loginRequestDTO);
		var inOrder = inOrder(authenticationManager, jwtService);
		inOrder.verify(authenticationManager).authenticate(any());
		inOrder.verify(jwtService).generateToken(any());
	}

	/**
	 * Ensures authentication principal is resolved before token generation.
	 */
	@Test
	@Order(14)
	@DisplayName("should get principal BEFORE generating a JWT token")
	void login_GetsPrincipalBeforeGeneratingToken() {
		authenticate();
		loginUserService.login(loginRequestDTO);
		var inOrder = inOrder(authentication, jwtService);
		inOrder.verify(authentication).getPrincipal();
		inOrder.verify(jwtService).generateToken(any());
	}

	/**
	 * Ensures BadCredentialsException is propagated when credentials are invalid.
	 */
	@Test
	@Order(15)
	@DisplayName("should propagate BadCredentialsException for wrong password")
	void login_WrongPassword_ThrowsBadCredentials() {
		given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException("Bad credentials"));
		assertThatThrownBy(() -> loginUserService.login(loginRequestDTO)).isInstanceOf(BadCredentialsException.class)
				.hasMessageContaining("Bad credentials");
	}

	/**
	 * Ensures BadCredentialsException is propagated for unknown users.
	 */
	@Test
	@Order(16)
	@DisplayName("should propagate BadCredentialsException for unknown email")
	void login_UnknownEmail_ThrowsBadCredentials() {
		UserLoginRequestDTO unknownRequest = new UserLoginRequestDTO("unknown@example.com", TEST_PASSWORD);
		given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException("Bad credentials"));
		assertThatThrownBy(() -> loginUserService.login(unknownRequest)).isInstanceOf(BadCredentialsException.class);
	}

	/**
	 * Ensures DisabledException is propagated for disabled accounts.
	 */
	@Test
	@Order(17)
	@DisplayName("should propagate DisabledException when account is disabled")
	void login_DisabledAccount_ThrowsDisabledException() {
		given(authenticationManager.authenticate(any())).willThrow(new DisabledException("Account disabled"));
		assertThatThrownBy(() -> loginUserService.login(loginRequestDTO)).isInstanceOf(DisabledException.class)
				.hasMessageContaining("Account disabled");
	}

	/**
	 * Ensures LockedException is propagated for locked accounts.
	 */
	@Test
	@Order(18)
	@DisplayName("should propagate LockedException when account is locked")
	void login_LockedAccount_ThrowsLockedException() {
		given(authenticationManager.authenticate(any())).willThrow(new LockedException("Account locked"));
		assertThatThrownBy(() -> loginUserService.login(loginRequestDTO)).isInstanceOf(LockedException.class)
				.hasMessageContaining("Account locked");
	}

	/**
	 * Ensures JwtService is never invoked when authentication fails.
	 */
	@Test
	@Order(19)
	@DisplayName("should NOT call jwtService when authentication fails")
	void login_AuthFails_JwtServiceNeverCalled() {
		given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException("Bad credentials"));
		assertThatThrownBy(() -> loginUserService.login(loginRequestDTO)).isInstanceOf(BadCredentialsException.class);
		verifyNoInteractions(jwtService);
	}

	/**
	 * Ensures authentication principal is not accessed when authentication fails.
	 */
	@Test
	@Order(20)
	@DisplayName("should NOT call getPrincipal() when authentication fails")
	void login_AuthFails_GetPrincipalNeverCalled() {
		given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException("Bad credentials"));

		assertThatThrownBy(() -> loginUserService.login(loginRequestDTO)).isInstanceOf(BadCredentialsException.class);

		verifyNoInteractions(authentication);
	}

	/**
	 * Ensures service throws NullPointerException when request is null.
	 */
	@Test
	@Order(21)
	@DisplayName("should throw NullPointerException when request is null")
	void login_NullRequest_ThrowsException() {
		assertThatThrownBy(() -> loginUserService.login(null)).isInstanceOf(NullPointerException.class);
	}

	/**
	 * Ensures invalid principal type results in ClassCastException.
	 */
	@Test
	@Order(22)
	@DisplayName("should propagate ClassCastException if principal is not CustomUserDetails")
	void login_PrincipalNotCustomUserDetails_ThrowsClassCastException() {
		Object wrongPrincipal = "not-a-CustomUserDetails-object";
		given(authenticationManager.authenticate(any())).willReturn(authentication);
		given(authentication.getPrincipal()).willReturn(wrongPrincipal);

		assertThatThrownBy(() -> loginUserService.login(loginRequestDTO)).isInstanceOf(ClassCastException.class);
	}

	/**
	 * Ensures no unexpected interactions with AuthenticationManager.
	 */
	@Test
	@Order(23)
	@DisplayName("should invoke no extra methods on authenticationManager")
	void login_NoExtraCallsOnAuthManager() {
		authenticate();
		loginUserService.login(loginRequestDTO);
		verify(authenticationManager, times(1)).authenticate(any());
		verifyNoMoreInteractions(authenticationManager);
	}

	/**
	 * Ensures no unexpected interactions with JwtService.
	 */
	@Test
	@Order(24)
	@DisplayName("should invoke no extra methods on jwtService")
	void login_NoExtraCallsOnJwtService() {
		authenticate();
		loginUserService.login(loginRequestDTO);
		verify(jwtService, times(1)).generateToken(any());
		verifyNoMoreInteractions(jwtService);
	}

	/**
	 * Ensures CustomUserDetails.getUser() is invoked exactly once per login.
	 */
	@Test
	@Order(25)
	@DisplayName("should call getUser() on CustomUserDetails exactly once")
	void login_CallsGetUserExactlyOnce() {
		authenticate();
		loginUserService.login(loginRequestDTO);
		verify(customUserDetails, times(1)).getUser();
	}
}
