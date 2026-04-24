package userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 *
 * Branches covered:
 *  1. Public endpoint  → filter skipped entirely (4 public path variants)
 *  2. No Authorization header           → passes through, no auth set
 *  3. Authorization header without "Bearer " prefix → passes through
 *  4. JWT extraction throws exception   → passes through, no auth set
 *  5. Extracted email is null           → passes through, no auth set
 *  6. SecurityContext already has auth  → passes through, no loadUser call
 *  7. Token valid                       → authentication set in SecurityContext
 *  8. Token invalid                     → passes through, no auth set
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class JwtAuthenticationFilterTest {

	@Mock private JwtService                jwtService;
	@Mock private CustomUserDetailsService  customUserDetailsService;

	@InjectMocks
	private JwtAuthenticationFilter filter;

	@Mock private HttpServletRequest  request;
	@Mock private HttpServletResponse response;
	@Mock private FilterChain         filterChain;
	@Mock private UserDetails         userDetails;
	@Mock private SecurityContext     securityContext;

	private static final String VALID_TOKEN  = "valid.jwt.token";
	private static final String BEARER_TOKEN = "Bearer " + VALID_TOKEN;
	private static final String USER_EMAIL   = "kinjal@example.com";
	private static final String SECURED_PATH = "/api/v1/users/profile";

	@BeforeEach
	void resetSecurityContext() {
		// Always start with a clean, unauthenticated SecurityContext
		SecurityContextHolder.clearContext();
	}

	// =========================================================================
	// 1. Public endpoints — filter skips JWT processing entirely
	// =========================================================================
	@Nested
	@DisplayName("public endpoint paths")
	class PublicEndpointTests {

		@ParameterizedTest(name = "path: {0}")
		@ValueSource(strings = {
				"/api/v1/users/login",
				"/api/v1/users/encode",
				"/swagger-ui/index.html",
				"/v3/api-docs/swagger-config"
		})
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

	// =========================================================================
	// 2. No Authorization header
	// =========================================================================
	@Nested
	@DisplayName("missing or malformed Authorization header")
	class AuthorizationHeaderTests {

		@BeforeEach
		void stubSecuredPath() {
			when(request.getServletPath()).thenReturn(SECURED_PATH);
		}

		@Test
		@DisplayName("null Authorization header — passes through")
		void nullHeader_passesThrough() throws Exception {
			when(request.getHeader("Authorization")).thenReturn(null);

			filter.doFilterInternal(request, response, filterChain);

			verify(filterChain).doFilter(request, response);
			verifyNoInteractions(jwtService, customUserDetailsService);
		}

		@Test
		@DisplayName("header present but no 'Bearer ' prefix — passes through")
		void headerWithoutBearerPrefix_passesThrough() throws Exception {
			when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

			filter.doFilterInternal(request, response, filterChain);

			verify(filterChain).doFilter(request, response);
			verifyNoInteractions(jwtService, customUserDetailsService);
		}

		@Test
		@DisplayName("header is empty string — passes through")
		void emptyHeader_passesThrough() throws Exception {
			when(request.getHeader("Authorization")).thenReturn("");

			filter.doFilterInternal(request, response, filterChain);

			verify(filterChain).doFilter(request, response);
			verifyNoInteractions(jwtService, customUserDetailsService);
		}
	}

	// =========================================================================
	// 3. JWT extraction throws exception
	// =========================================================================
	@Nested
	@DisplayName("JWT extraction failure")
	class JwtExtractionFailureTests {

		@Test
		@DisplayName("jwtService.extractUsername throws — passes through without auth")
		void extractUsernameThrows_passesThrough() throws Exception {
			when(request.getServletPath()).thenReturn(SECURED_PATH);
			when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
			when(jwtService.extractUsername(VALID_TOKEN))
					.thenThrow(new RuntimeException("Malformed JWT"));

			filter.doFilterInternal(request, response, filterChain);

			verify(filterChain).doFilter(request, response);
			verifyNoInteractions(customUserDetailsService);
			// SecurityContext must remain unauthenticated
			assertNoAuthenticationSet();
		}
	}

	// =========================================================================
	// 4. Extracted email is null
	// =========================================================================
	@Nested
	@DisplayName("extracted email is null")
	class NullEmailTests {

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

	// =========================================================================
	// 5. SecurityContext already authenticated
	// =========================================================================
	@Nested
	@DisplayName("SecurityContext already has authentication")
	class AlreadyAuthenticatedTests {

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

	// =========================================================================
	// 6. Valid token — authentication IS set in SecurityContext
	// =========================================================================
	@Nested
	@DisplayName("valid JWT token")
	class ValidTokenTests {

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

	// =========================================================================
	// 7. Invalid token — authentication NOT set
	// =========================================================================
	@Nested
	@DisplayName("invalid JWT token")
	class InvalidTokenTests {

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

	// =========================================================================
	// Helper
	// =========================================================================
	private void assertNoAuthenticationSet() {
		org.assertj.core.api.Assertions
				.assertThat(SecurityContextHolder.getContext().getAuthentication())
				.isNull();
	}
}