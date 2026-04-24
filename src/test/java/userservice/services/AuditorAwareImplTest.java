package userservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.security.CustomUserDetails;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Unit test class for {@code AuditorAwareImpl}.
 *
 * <p>This test suite validates the behavior of the auditing mechanism used by
 * Spring Data JPA to automatically populate {@code createdBy} and
 * {@code updatedBy} fields based on the currently authenticated user.
 *
 * <p><b>Testing Scope:</b>
 * <ul>
 *   <li>Service logic for retrieving the current auditor (user ID)</li>
 *   <li>Integration with {@link SecurityContextHolder}</li>
 *   <li>Handling of different authentication and principal scenarios</li>
 * </ul>
 *
 * <p><b>Configuration:</b>
 * <ul>
 *   <li>Uses Mockito for mocking dependencies and security context</li>
 *   <li>{@code @ActiveProfiles("test")} enables test-specific configuration</li>
 *   <li>{@link MockitoAnnotations#openMocks(Object)} initializes mocks</li>
 * </ul>
 *
 * <p><b>Mocked Dependencies:</b>
 * <ul>
 *   <li>{@link UserRepository} – (not directly used but part of class dependency)</li>
 *   <li>{@link SecurityContext} and {@link Authentication} – simulate security context behavior</li>
 * </ul>
 *
 * <p><b>Key Test Scenarios:</b>
 * <ul>
 *   <li><b>Authenticated User Present:</b>
 *       <ul>
 *         <li>Returns {@code Optional.of(userId)}</li>
 *         <li>Extracts user ID from {@link CustomUserDetails}</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Not Authenticated:</b>
 *       <ul>
 *         <li>Returns {@code Optional.empty()}</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Anonymous User:</b>
 *       <ul>
 *         <li>Returns {@code Optional.of(0L)} as default system/anonymous identifier</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Invalid Principal:</b>
 *       <ul>
 *         <li>Handles cases where principal is not {@link CustomUserDetails}</li>
 *         <li>Returns {@code Optional.empty()}</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Null Authentication:</b>
 *       <ul>
 *         <li>Handles missing security context gracefully</li>
 *         <li>Returns {@code Optional.empty()}</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p><b>Assertions:</b>
 * <ul>
 *   <li>Verifies presence or absence of auditor ID using {@link Optional}</li>
 *   <li>Ensures correct user ID extraction</li>
 * </ul>
 *
 * <p><b>Design Notes:</b>
 * <ul>
 *   <li>Ensures robustness of auditing logic across edge cases</li>
 *   <li>Prevents runtime failures due to invalid security context states</li>
 *   <li>Supports consistent population of audit fields in entities</li>
 * </ul>
 */
@ActiveProfiles("test")
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
		MockitoAnnotations.openMocks(this);
		SecurityContextHolder.setContext(securityContext);
	}

	@Test
	void shouldReturnUserId_whenAuthenticatedUserPresent() {
		// Arrange
		User user = new User();
		user.setId(10L);

		CustomUserDetails userDetails = mock(CustomUserDetails.class);
		when(userDetails.getUser()).thenReturn(user);

		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.isAuthenticated()).thenReturn(true);
		when(authentication.getName()).thenReturn("validUser"); // ✅ important
		when(authentication.getPrincipal()).thenReturn(userDetails);

		// Act
		Optional<Long> result = auditorAware.getCurrentAuditor();

		// Assert
		assertTrue(result.isPresent());
		assertEquals(10L, result.get());
	}

	@Test
	void shouldReturnEmpty__whenNotAuthenticated() {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.isAuthenticated()).thenReturn(false);

		Optional<Long> result = auditorAware.getCurrentAuditor();

		assertFalse(result.isPresent());
	}
	@Test
	void shouldReturnEmpty_whenPrincipalIsNotCustomUserDetails() {
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.isAuthenticated()).thenReturn(true);
		when(authentication.getName()).thenReturn("validUser");
		when(authentication.getPrincipal()).thenReturn(new Object());

		Optional<Long> result = auditorAware.getCurrentAuditor();

		assertFalse(result.isPresent());
	}
	@Test
	void shouldReturnZero__whenAnonymousUser() {
		// Arrange
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.isAuthenticated()).thenReturn(true);
		when(authentication.getName()).thenReturn("anonymousUser");

		// Act
		Optional<Long> result = auditorAware.getCurrentAuditor();

		// Assert
		assertTrue(result.isPresent());
		assertEquals(0L, result.get());
	}

	@Test
	void shouldReturnZero_whenAnonymousUser() {
		// Arrange
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.isAuthenticated()).thenReturn(true);
		when(authentication.getName()).thenReturn("anonymousUser");

		// IMPORTANT: avoid ClassCastException path
		when(authentication.getPrincipal()).thenReturn(null);

		// Act
		Optional<Long> result = auditorAware.getCurrentAuditor();

		// Assert
		assertTrue(result.isPresent());
		assertEquals(0L, result.get());
	}

	@Test
	void shouldReturnEmpty_whenPrincipalIsInvalid() {
		// Arrange
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.isAuthenticated()).thenReturn(true);
		when(authentication.getName()).thenReturn("validUser");
		when(authentication.getPrincipal()).thenReturn("invalidPrincipal");

		// Act
		Optional<Long> result = auditorAware.getCurrentAuditor();

		// Assert
		assertFalse(result.isPresent()); //  correct expectation now
	}

	@Test
	void shouldReturnEmpty_whenNotAuthenticated() {
		// Arrange
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.isAuthenticated()).thenReturn(false);

		// Act
		Optional<Long> result = auditorAware.getCurrentAuditor();

		// Assert
		assertFalse(result.isPresent());
	}

	@Test
	void shouldReturnEmpty_whenAuthNotAuthenticated() {
		// Arrange
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.isAuthenticated()).thenReturn(false);

		// Act
		Optional<Long> result = auditorAware.getCurrentAuditor();

		// Assert
		assertFalse(result.isPresent());
	}

	@Test
	void shouldReturnEmpty_whenAuthIsNull() {
		// Arrange
		SecurityContextHolder.clearContext(); //  important: ensures auth is truly null

		// Act
		Optional<Long> result = auditorAware.getCurrentAuditor();

		// Assert
		assertFalse(result.isPresent());
	}
}