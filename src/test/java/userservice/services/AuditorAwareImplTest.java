package userservice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.security.CustomUserDetails;
import userservice.services.impls.AuditorAwareImpl;

/**
 * Verifies AuditorAware implementation used for Spring Data auditing.
 *
 * Security/behavior expectations: - Authenticated users must return their user
 * ID as auditor - Unauthenticated or invalid authentication must return empty -
 * Only valid CustomUserDetails should be accepted as principal -
 * SecurityContext must be safely handled in all scenarios
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AuditorAwareImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private SecurityContext securityContext;

	@Mock
	private Authentication authentication;

	@InjectMocks
	private AuditorAwareImpl auditorAware;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.setContext(securityContext);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	/**
	 * Ensures that an authenticated user is correctly resolved as auditor.
	 *
	 * Expected behavior: - User ID is extracted from CustomUserDetails - Optional
	 * contains user ID
	 */
	@Test
	@DisplayName("authenticated user — returns user ID")
	void authenticatedUser_returnsUserId() {
		User user = new User();
		user.setId(10L);

		CustomUserDetails userDetails = mock(CustomUserDetails.class);
		when(userDetails.getUser()).thenReturn(user);

		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.isAuthenticated()).thenReturn(true);
		when(authentication.getPrincipal()).thenReturn(userDetails);

		Optional<Long> result = auditorAware.getCurrentAuditor();

		assertTrue(result.isPresent());
		assertEquals(10L, result.get());
	}

	/**
	 * Ensures that invalid authentication states return empty auditor.
	 *
	 * Covers: - Null authentication - Authentication marked as not authenticated
	 */
	@ParameterizedTest(name = "{index} — {0}")
	@DisplayName("unauthenticated scenarios — returns empty")
	@MethodSource("unauthenticatedArgs")
	void unauthenticated_returnsEmpty(Authentication auth) { // ✅ no String parameter
		when(securityContext.getAuthentication()).thenReturn(auth);

		Optional<Long> result = auditorAware.getCurrentAuditor();

		assertFalse(result.isPresent());
	}

	/**
	 * Provides invalid authentication scenarios.
	 */
	static Stream<Arguments> unauthenticatedArgs() {
		Authentication notAuthenticated = mock(Authentication.class);
		when(notAuthenticated.isAuthenticated()).thenReturn(false);

		return Stream.of(Arguments.of(Named.of("null authentication", (Authentication) null)), // ✅ cast null
				Arguments.of(Named.of("not authenticated", notAuthenticated)));
	}

	/**
	 * Ensures that non-UserDetails principals are rejected safely.
	 *
	 * Expected behavior: - Principal must be instance of CustomUserDetails -
	 * Otherwise, auditor is empty
	 */
	@Test
	@DisplayName("principal is not CustomUserDetails — returns empty")
	void invalidPrincipal_returnsEmpty() {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.isAuthenticated()).thenReturn(true);
		when(authentication.getPrincipal()).thenReturn(new Object());

		Optional<Long> result = auditorAware.getCurrentAuditor();

		assertFalse(result.isPresent());
	}

	/**
	 * Ensures that missing SecurityContext returns empty auditor safely.
	 *
	 * Expected behavior: - No NullPointerException - Empty Optional returned
	 */
	@Test
	@DisplayName("null authentication — returns empty")
	void nullAuthentication_returnsEmpty() {
		SecurityContextHolder.clearContext();

		Optional<Long> result = auditorAware.getCurrentAuditor();

		assertFalse(result.isPresent());
	}
}