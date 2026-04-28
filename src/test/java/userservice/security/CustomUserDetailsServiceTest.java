package userservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import userservice.models.User;
import userservice.repositories.UserRepository;

/**
 * Unit tests for {@link CustomUserDetailsService}.
 *
 * Branches covered: 1. User found → returns CustomUserDetails wrapping the User
 * 2. User not found → throws UsernameNotFoundException with correct message
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class CustomUserDetailsServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CustomUserDetailsService customUserDetailsService;

	private static final String EMAIL = "kinjal@example.com";
	private static final String UNKNOWN = "ghost@example.com";

	/**
	 * Test suite validating successful user lookup behavior in
	 * {@code loadUserByUsername()}.
	 *
	 * <p>
	 * Ensures that when a matching user exists, the service returns a fully
	 * populated {@link CustomUserDetails} object.
	 */
	@Nested
	@DisplayName("loadUserByUsername — user found")
	class UserFoundTests {

		/**
		 * Verifies that an existing user is wrapped inside {@link CustomUserDetails}
		 * and returned successfully.
		 *
		 * <p>
		 * Expected behavior:
		 * <ul>
		 * <li>Repository finds user by email</li>
		 * <li>Returned object is CustomUserDetails</li>
		 * <li>Wrapped User instance matches repository result</li>
		 * <li>Username equals supplied email</li>
		 * </ul>
		 */
		@Test
		@DisplayName("returns CustomUserDetails wrapping the found User")
		void userFound_returnsCustomUserDetails() {
			User user = new User();
			user.setId(1L);
			user.setEmail(EMAIL);

			when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

			UserDetails result = customUserDetailsService.loadUserByUsername(EMAIL);

			// Must return a CustomUserDetails instance
			assertThat(result).isInstanceOf(CustomUserDetails.class);

			// The wrapped User must be the exact one returned by the repository
			CustomUserDetails customUserDetails = (CustomUserDetails) result;
			assertThat(customUserDetails.getUser()).isEqualTo(user);
			assertThat(customUserDetails.getUsername()).isEqualTo(EMAIL);

			verify(userRepository).findByEmail(EMAIL);
		}
	}

	/**
	 * Test suite validating failure behavior when user lookup does not return a
	 * match.
	 *
	 * <p>
	 * Ensures Spring Security receives a proper {@link UsernameNotFoundException}.
	 */
	@Nested
	@DisplayName("loadUserByUsername — user not found")
	class UserNotFoundTests {

		/**
		 * Verifies that an unknown email triggers {@link UsernameNotFoundException}
		 * with a descriptive message.
		 *
		 * <p>
		 * Expected behavior:
		 * <ul>
		 * <li>Repository returns empty result</li>
		 * <li>Exception type is UsernameNotFoundException</li>
		 * <li>Error message contains requested username/email</li>
		 * </ul>
		 */
		@Test
		@DisplayName("throws UsernameNotFoundException with descriptive message")
		void userNotFound_throwsUsernameNotFoundException() {
			when(userRepository.findByEmail(UNKNOWN)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(UNKNOWN))
					.isInstanceOf(UsernameNotFoundException.class).hasMessageContaining(UNKNOWN);

			verify(userRepository).findByEmail(UNKNOWN);
		}
	}
}