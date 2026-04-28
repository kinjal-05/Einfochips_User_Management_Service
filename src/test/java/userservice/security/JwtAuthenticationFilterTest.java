package userservice.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 *
 * This test suite validates all major authentication filter branches including:
 * <ul>
 * <li>Public endpoints bypassing JWT processing</li>
 * <li>Missing or malformed Authorization headers</li>
 * <li>JWT parsing and extraction failures</li>
 * <li>Null or missing username extraction from token</li>
 * <li>Pre-authenticated SecurityContext behavior</li>
 * <li>Successful authentication flow with valid JWT</li>
 * <li>Invalid JWT handling without authentication leakage</li>
 * </ul>
 *
 * The primary goal is to ensure:
 * <ul>
 * <li>SecurityContext is only populated for valid tokens</li>
 * <li>No unnecessary service calls occur in bypass scenarios</li>
 * <li>Filter chain is always continued</li>
 * <li>No authentication state leaks between requests</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class JwtAuthenticationFilterTest {

	@Mock
	private JwtService jwtService;
	@Mock
	private CustomUserDetailsService customUserDetailsService;

	@InjectMocks
	private JwtAuthenticationFilter filter;

	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpServletResponse response;
	@Mock
	private FilterChain filterChain;
	@Mock
	private UserDetails userDetails;
	@Mock
	private SecurityContext securityContext;

	private static final String VALID_TOKEN = "valid.jwt.token";
	private static final String BEARER_TOKEN = "Bearer " + VALID_TOKEN;
	private static final String USER_EMAIL = "kinjal@example.com";
	private static final String SECURED_PATH = "/api/v1/users/profile";

	@BeforeEach
	void resetSecurityContext() {
		// Always start with a clean, unauthenticated SecurityContext
		SecurityContextHolder.clearContext();
	}

	/**
	 * Verifies that public endpoints bypass JWT authentication processing entirely.
	 *
	 * Expected behavior: - Filter chain proceeds without interruption - No
	 * Authorization header is accessed - No JWT parsing or user lookup is performed
	 */
	@Nested
	@DisplayName("public endpoint paths")
	class PublicEndpointTests {

		/**
		 * Verifies that public endpoints bypass JWT authentication processing entirely.
		 *
		 * Expected behavior: - The request is forwarded through the filter chain
		 * without interruption - No Authorization header is accessed from the incoming
		 * request - No JWT parsing or authentication logic is triggered for public
		 * paths
		 */
		@ParameterizedTest(name = "path: {0}")
		@ValueSource(strings = { "/api/v1/users/login", "/api/v1/users/encode", "/swagger-ui/index.html",
				"/v3/api-docs/swagger-config" })
		@DisplayName("passes through without reading Authorization header")
		void publicPath_skipsFilter(String publicPath) throws Exception {
			when(request.getServletPath()).thenReturn(publicPath);

			filter.doFilterInternal(request, response, filterChain);

			// Must pass request along
			verify(filterChain).doFilter(request, response);
			// Must NOT touch the Authorization header at all
			verify(request, never()).getHeader(anyString());
			// Must NOT attempt any JWT parsing
			verifyNoInteractions(jwtService, customUserDetailsService);
		}
	}

	/**
	 * Verifies behavior when Authorization header is missing or malformed.
	 *
	 * Security expectation: - Requests should not fail or throw exceptions - Filter
	 * must allow request to proceed through filter chain - No JWT validation or
	 * user lookup should be triggered - Authentication processing is skipped safely
	 * for invalid headers
	 */
	@Nested
	@DisplayName("missing or malformed Authorization header")
	class AuthorizationHeaderTests {

		@BeforeEach
		void stubSecuredPath() {
			when(request.getServletPath()).thenReturn(SECURED_PATH);
		}

		/**
		 * Ensures that invalid Authorization header values (null, empty, or blank) do
		 * not trigger authentication logic and are safely ignored.
		 *
		 * Expected behavior: - Request proceeds through filter chain without
		 * interruption - JWT parsing is not attempted - User details service is not
		 * invoked
		 */
		@ParameterizedTest(name = "{index} — {0}")
		@DisplayName("invalid Authorization header — passes through")
		@NullAndEmptySource
		@ValueSource(strings = { "   " })
		void invalidHeader_passesThrough(String headerValue) throws Exception {
			when(request.getHeader("Authorization")).thenReturn(headerValue);

			filter.doFilterInternal(request, response, filterChain);

			verify(filterChain).doFilter(request, response);
			verifyNoInteractions(jwtService, customUserDetailsService);
		}

	}

	/**
	 * Verifies behavior when JWT extraction fails during authentication processing.
	 *
	 * Security expectation: - JWT parsing errors must not break request flow -
	 * Request must continue through filter chain - No user authentication must be
	 * attempted - SecurityContext must remain unauthenticated
	 */
	@Nested
	@DisplayName("JWT extraction failure")
	class JwtExtractionFailureTests {

		/**
		 * Ensures that exceptions thrown while extracting username from JWT do not
		 * interrupt request processing or trigger authentication.
		 *
		 * Expected behavior: - Request proceeds through filter chain - No call to
		 * UserDetailsService is made - SecurityContext remains empty (unauthenticated)
		 */
		@Test
		@DisplayName("jwtService.extractUsername throws — passes through without auth")
		void extractUsernameThrows_passesThrough() throws Exception {
			when(request.getServletPath()).thenReturn(SECURED_PATH);
			when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
			when(jwtService.extractUsername(VALID_TOKEN)).thenThrow(new RuntimeException("Malformed JWT"));

			filter.doFilterInternal(request, response, filterChain);

			verify(filterChain).doFilter(request, response);
			verifyNoInteractions(customUserDetailsService);
			// SecurityContext must remain unauthenticated
			assertNoAuthenticationSet();
		}
	}

	/**
	 * Verifies behavior when JWT token is valid but contains no username/email.
	 *
	 * Security expectation: - Null identity must not trigger authentication flow -
	 * No user lookup should be performed - Request must proceed normally without
	 * authentication
	 */
	@Nested
	@DisplayName("extracted email is null")
	class NullEmailTests {

		/**
		 * Ensures that a null username extracted from JWT is treated as an
		 * unauthenticated request.
		 *
		 * Expected behavior: - Filter chain continues execution - UserDetailsService is
		 * not invoked - SecurityContext remains unauthenticated
		 */
		@Test
		@DisplayName("null email — passes through, no loadUser call")
		void nullEmail_passesThrough() throws Exception {
			when(request.getServletPath()).thenReturn(SECURED_PATH);
			when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
			when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(null);

			filter.doFilterInternal(request, response, filterChain);

			verify(filterChain).doFilter(request, response);
			verifyNoInteractions(customUserDetailsService);
			assertNoAuthenticationSet();
		}
	}

	/**
	 * Verifies behavior when SecurityContext already contains an authenticated
	 * user.
	 *
	 * Security expectation: - Existing authentication must not be overridden - JWT
	 * validation must be skipped entirely - No user lookup or token verification
	 * should occur - Filter must continue request processing normally
	 */
	@Nested
	@DisplayName("SecurityContext already has authentication")
	class AlreadyAuthenticatedTests {

		/**
		 * Ensures that when a user is already authenticated in the SecurityContext, the
		 * JWT filter does not re-process or re-validate the token.
		 *
		 * Expected behavior: - Request proceeds through filter chain -
		 * UserDetailsService is not invoked - JWT validation is not performed
		 */
		@Test
		@DisplayName("existing auth — skips loadUser and token validation")
		void alreadyAuthenticated_skipsValidation() throws Exception {
			// Arrange — pre-populate SecurityContext with an existing auth
			Authentication existingAuth = mock(Authentication.class);
			SecurityContext ctx = SecurityContextHolder.createEmptyContext();
			ctx.setAuthentication(existingAuth);
			SecurityContextHolder.setContext(ctx);

			when(request.getServletPath()).thenReturn(SECURED_PATH);
			when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
			when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(USER_EMAIL);

			filter.doFilterInternal(request, response, filterChain);

			verify(filterChain).doFilter(request, response);
			verifyNoInteractions(customUserDetailsService);
			verify(jwtService, never()).isTokenValid(any(), any());
		}
	}

	/**
	 * Verifies behavior when a valid JWT token is successfully processed.
	 *
	 * Security expectation: - Authentication must be created and stored in
	 * SecurityContext - UserDetails must be loaded from persistence layer - Token
	 * must be validated before authentication is set - Filter chain must continue
	 * after successful authentication
	 */
	@Nested
	@DisplayName("valid JWT token")
	class ValidTokenTests {

		/**
		 * Ensures that a valid JWT results in proper authentication being set in the
		 * SecurityContext.
		 *
		 * Expected behavior: - User is authenticated using
		 * UsernamePasswordAuthenticationToken - UserDetailsService is invoked once -
		 * JWT validation is performed - SecurityContext is populated with authenticated
		 * principal
		 */
		@Test
		@DisplayName("valid token — UsernamePasswordAuthenticationToken set in SecurityContext")
		void validToken_setsAuthentication() throws Exception {
			when(request.getServletPath()).thenReturn(SECURED_PATH);
			when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
			when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(USER_EMAIL);
			when(customUserDetailsService.loadUserByUsername(USER_EMAIL)).thenReturn(userDetails);
			when(jwtService.isTokenValid(VALID_TOKEN, userDetails)).thenReturn(true);
			when(userDetails.getAuthorities()).thenReturn(java.util.Collections.emptyList());

			filter.doFilterInternal(request, response, filterChain);

			// Authentication must be set
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			org.assertj.core.api.Assertions.assertThat(auth).isNotNull();
			org.assertj.core.api.Assertions.assertThat(auth.getPrincipal()).isEqualTo(userDetails);

			verify(customUserDetailsService).loadUserByUsername(USER_EMAIL);
			verify(jwtService).isTokenValid(VALID_TOKEN, userDetails);
			verify(filterChain).doFilter(request, response);
		}
	}

	/**
	 * Verifies behavior when JWT token is present but invalid.
	 *
	 * Security expectation: - Authentication must not be set in SecurityContext -
	 * Token validation failure must not crash request flow - Filter chain must
	 * still execute normally
	 */
	@Nested
	@DisplayName("invalid JWT token")
	class InvalidTokenTests {

		/**
		 * Ensures that invalid JWT tokens do not result in authentication being set in
		 * the SecurityContext.
		 *
		 * Expected behavior: - No authentication is created - Request continues through
		 * filter chain - SecurityContext remains empty
		 */
		@Test
		@DisplayName("invalid token — SecurityContext remains unauthenticated")
		void invalidToken_noAuthSet() throws Exception {
			when(request.getServletPath()).thenReturn(SECURED_PATH);
			when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
			when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(USER_EMAIL);
			when(customUserDetailsService.loadUserByUsername(USER_EMAIL)).thenReturn(userDetails);
			when(jwtService.isTokenValid(VALID_TOKEN, userDetails)).thenReturn(false);

			filter.doFilterInternal(request, response, filterChain);

			assertNoAuthenticationSet();
			verify(filterChain).doFilter(request, response);
		}
	}

	/**
	 * Utility assertion to verify that SecurityContext is unauthenticated.
	 */
	private void assertNoAuthenticationSet() {
		org.assertj.core.api.Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}
}