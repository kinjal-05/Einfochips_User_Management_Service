package userservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import userservice.Utility.MapToUserResponseDTO;
import userservice.dtos.LoginRequestDTO;
import userservice.dtos.LoginResponseDTO;
import userservice.enums.Role;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.security.CustomUserDetails;
import userservice.security.JwtService;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;


/**
 * Comprehensive unit test suite for {@link LoginUserServiceImpl#login(LoginRequestDTO)}.
 *
 * <p>This test class validates the login/authentication workflow in isolation
 * using Mockito and JUnit 5. It ensures correct authentication handling,
 * JWT generation, response construction, and robust error propagation.</p>
 *
 * <h3>Test Coverage</h3>
 * <ul>
 *   <li><b>Happy Path:</b>
 *       Verifies successful authentication and ensures a fully populated
 *       {@link LoginResponseDTO} is returned.</li>
 *
 *   <li><b>Authentication Flow:</b>
 *       <ul>
 *           <li>Ensures credentials are passed correctly to {@link AuthenticationManager}</li>
 *           <li>Validates {@link Authentication#getPrincipal()} is used to extract user details</li>
 *           <li>Confirms proper casting to {@link CustomUserDetails}</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>JWT Token Handling:</b>
 *       <ul>
 *           <li>Ensures token is generated using {@link JwtService}</li>
 *           <li>Validates token is embedded correctly in the response DTO</li>
 *           <li>Confirms token generation occurs only after successful authentication</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Response Validation:</b>
 *       <ul>
 *           <li>Verifies correctness of user ID, email, role, and message</li>
 *           <li>Ensures response is never null</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Interaction Verification:</b>
 *       <ul>
 *           <li>Ensures {@code authenticate()} is called exactly once</li>
 *           <li>Ensures {@code generateToken()} is called exactly once</li>
 *           <li>Validates correct order of operations (authenticate → principal → token)</li>
 *           <li>Confirms no unnecessary interactions with dependencies</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Exception Handling:</b>
 *       <ul>
 *           <li>Propagates {@link BadCredentialsException} for invalid credentials</li>
 *           <li>Handles {@link DisabledException} for disabled accounts</li>
 *           <li>Handles {@link LockedException} for locked accounts</li>
 *           <li>Propagates {@link ClassCastException} for invalid principal type</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Failure Scenarios:</b>
 *       <ul>
 *           <li>Ensures JWT generation is skipped when authentication fails</li>
 *           <li>Ensures principal extraction is skipped on failure</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Edge Cases:</b>
 *       <ul>
 *           <li>Null request handling</li>
 *           <li>Invalid principal type handling</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>Testing Strategy</h3>
 * <ul>
 *   <li>Uses {@link MockitoExtension} for mock initialization</li>
 *   <li>Follows Arrange-Act-Assert pattern</li>
 *   <li>Uses helper methods for reusable authentication stubbing</li>
 *   <li>Captures arguments to validate correctness of authentication requests</li>
 * </ul>
 *
 * <h3>Key Design Considerations</h3>
 * <ul>
 *   <li>Ensures strict isolation of authentication logic</li>
 *   <li>Validates secure handling of credentials and tokens</li>
 *   <li>Prevents regression in login and security workflows</li>
 *   <li>Maintains high readability and maintainability</li>
 * </ul>
 *
 * <p>This test suite is designed to meet production-grade standards and ensure
 * reliability, security, and correctness of the login functionality.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - login()")
@ActiveProfiles("test")
public class LoginUserServiceTest {
	@Mock private UserRepository userRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private AuthenticationManager authenticationManager;
	@Mock private JwtService jwtService;
	@Mock private Authentication authentication;
	@Mock private CustomUserDetails customUserDetails;
	@Mock private SecurityContext securityContext;
	@Mock private MapToUserResponseDTO mapToUserResponseDTO;;
	// Service under test
	@InjectMocks
	private LoginUserServiceImpl loginUserService;

	private static final Long   USER_ID       = 10L;
	private static final String TEST_EMAIL    = "john.doe@example.com";
	private static final String TEST_PASSWORD = "Secret@123";
	private static final String JWT_TOKEN     = "eyJhbGciOiJIUzI1NiJ9.test.token";
	private static final Role TEST_ROLE     = Role.ROLE_USER;

	private LoginRequestDTO loginRequestDTO;
	private User savedUser;

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
		LoginResponseDTO result=loginUserService.login(loginRequestDTO);
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
		LoginResponseDTO result=loginUserService.login(loginRequestDTO);
		assertThat(result.token()).isEqualTo(JWT_TOKEN);
	}

	@Test
	@Order(3)
	@DisplayName("should return the user id from the authenticated principal")
	void login_ReturnsCorrectUserId() {
		authenticate();
		LoginResponseDTO result=loginUserService.login(loginRequestDTO);
		assertThat(result.id()).isEqualTo(USER_ID);
	}

	@Test
	@Order(4)
	@DisplayName("should return the email from the authenticated User entity")
	void login_ReturnsCorrectEmail() {
		authenticate();
		LoginResponseDTO result=loginUserService.login(loginRequestDTO);
		assertThat(result.email()).isEqualTo(TEST_EMAIL);
	}

	@Test
	@Order(5)
	@DisplayName("should return the role from the authenticated User entity")
	void login_ReturnsCorrectRole() {
		authenticate();
		LoginResponseDTO result=loginUserService.login(loginRequestDTO);
		assertThat(result.role()).isEqualTo(TEST_ROLE);
	}

	@Test
	@Order(6)
	@DisplayName("should never return null")
	void login_NeverReturnsNull() {
		authenticate();
		LoginResponseDTO result=loginUserService.login(loginRequestDTO);
		assertThat(result).isNotNull();
	}

	@Test
	@Order(7)
	@DisplayName("should pass email and password from the request to authenticationManager")
	void login_PassesCorrectCredentialsToAuthManager() {
		authenticate();
		loginUserService.login(loginRequestDTO);
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
		loginUserService.login(loginRequestDTO);
		verify(authenticationManager, times(1))
				.authenticate(any(UsernamePasswordAuthenticationToken.class));
	}

	@Test
	@Order(9)
	@DisplayName("should cast authentication principal to CustomUserDetails")
	void login_CastsPrincipalToCustomUserDetails() {
		authenticate();
		loginUserService.login(loginRequestDTO);
		// getPrincipal() must be called to obtain CustomUserDetails
		verify(authentication, times(1)).getPrincipal();
	}

	@Test
	@Order(10)
	@DisplayName("should call jwtService.generateToken() exactly once with CustomUserDetails")
	void login_CallsJwtServiceExactlyOnce() {
		authenticate();
		loginUserService.login(loginRequestDTO);
		verify(jwtService, times(1)).generateToken(customUserDetails);
		verifyNoMoreInteractions(jwtService);
	}

	@Test
	@Order(11)
	@DisplayName("should pass the principal (CustomUserDetails) to jwtService, not the raw user")
	void login_PassesCustomUserDetailsTOJwtService() {
		authenticate();
		loginUserService.login(loginRequestDTO);
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

		LoginResponseDTO result = loginUserService.login(loginRequestDTO);

		assertThat(result.token()).isEqualTo(customToken);
	}

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

	@Test
	@Order(15)
	@DisplayName("should propagate BadCredentialsException for wrong password")
	void login_WrongPassword_ThrowsBadCredentials() {
		given(authenticationManager.authenticate(any()))
				.willThrow(new BadCredentialsException("Bad credentials"));
		assertThatThrownBy(() -> loginUserService.login(loginRequestDTO))
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
		assertThatThrownBy(() -> loginUserService.login(unknownRequest))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	@Order(17)
	@DisplayName("should propagate DisabledException when account is disabled")
	void login_DisabledAccount_ThrowsDisabledException() {
		given(authenticationManager.authenticate(any()))
				.willThrow(new DisabledException("Account disabled"));
		assertThatThrownBy(() -> loginUserService.login(loginRequestDTO))
				.isInstanceOf(DisabledException.class)
				.hasMessageContaining("Account disabled");
	}

	@Test
	@Order(18)
	@DisplayName("should propagate LockedException when account is locked")
	void login_LockedAccount_ThrowsLockedException() {
		given(authenticationManager.authenticate(any()))
				.willThrow(new LockedException("Account locked"));
		assertThatThrownBy(() -> loginUserService.login(loginRequestDTO))
				.isInstanceOf(LockedException.class)
				.hasMessageContaining("Account locked");
	}

	@Test
	@Order(19)
	@DisplayName("should NOT call jwtService when authentication fails")
	void login_AuthFails_JwtServiceNeverCalled() {
		given(authenticationManager.authenticate(any()))
				.willThrow(new BadCredentialsException("Bad credentials"));
		assertThatThrownBy(() -> loginUserService.login(loginRequestDTO))
				.isInstanceOf(BadCredentialsException.class);
		verifyNoInteractions(jwtService);
	}

	@Test
	@Order(20)
	@DisplayName("should NOT call getPrincipal() when authentication fails")
	void login_AuthFails_GetPrincipalNeverCalled() {
		given(authenticationManager.authenticate(any()))
				.willThrow(new BadCredentialsException("Bad credentials"));

		assertThatThrownBy(() -> loginUserService.login(loginRequestDTO))
				.isInstanceOf(BadCredentialsException.class);

		verifyNoInteractions(authentication);
	}

	@Test
	@Order(21)
	@DisplayName("should throw NullPointerException when request is null")
	void login_NullRequest_ThrowsException() {
		assertThatThrownBy(() -> loginUserService.login(null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	@Order(22)
	@DisplayName("should propagate ClassCastException if principal is not CustomUserDetails")
	void login_PrincipalNotCustomUserDetails_ThrowsClassCastException() {
		Object wrongPrincipal = "not-a-CustomUserDetails-object";
		given(authenticationManager.authenticate(any())).willReturn(authentication);
		given(authentication.getPrincipal()).willReturn(wrongPrincipal);

		assertThatThrownBy(() -> loginUserService.login(loginRequestDTO))
				.isInstanceOf(ClassCastException.class);
	}

	@Test
	@Order(23)
	@DisplayName("should invoke no extra methods on authenticationManager")
	void login_NoExtraCallsOnAuthManager() {
		authenticate();
		loginUserService.login(loginRequestDTO);
		verify(authenticationManager, times(1)).authenticate(any());
		verifyNoMoreInteractions(authenticationManager);
	}

	@Test
	@Order(24)
	@DisplayName("should invoke no extra methods on jwtService")
	void login_NoExtraCallsOnJwtService() {
		authenticate();
		loginUserService.login(loginRequestDTO);
		verify(jwtService, times(1)).generateToken(any());
		verifyNoMoreInteractions(jwtService);
	}

	@Test
	@Order(25)
	@DisplayName("should call getUser() on CustomUserDetails exactly once")
	void login_CallsGetUserExactlyOnce() {
		authenticate();
		loginUserService.login(loginRequestDTO);
		verify(customUserDetails, times(1)).getUser();
	}
}
