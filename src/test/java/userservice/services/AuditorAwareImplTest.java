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