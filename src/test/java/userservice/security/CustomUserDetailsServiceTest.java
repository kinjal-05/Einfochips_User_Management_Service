package userservice.security;

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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CustomUserDetailsService}.
 *
 * Branches covered:
 *  1. User found     → returns CustomUserDetails wrapping the User
 *  2. User not found → throws UsernameNotFoundException with correct message
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class CustomUserDetailsServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CustomUserDetailsService customUserDetailsService;

	private static final String EMAIL       = "kinjal@example.com";
	private static final String UNKNOWN     = "ghost@example.com";

	// =========================================================================
	// Branch 1 — user exists
	// =========================================================================
	@Nested
	@DisplayName("loadUserByUsername — user found")
	class UserFoundTests {

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

	// =========================================================================
	// Branch 2 — user not found
	// =========================================================================
	@Nested
	@DisplayName("loadUserByUsername — user not found")
	class UserNotFoundTests {

		@Test
		@DisplayName("throws UsernameNotFoundException with descriptive message")
		void userNotFound_throwsUsernameNotFoundException() {
			when(userRepository.findByEmail(UNKNOWN)).thenReturn(Optional.empty());

			assertThatThrownBy(() ->
					customUserDetailsService.loadUserByUsername(UNKNOWN))
					.isInstanceOf(UsernameNotFoundException.class)
					.hasMessageContaining(UNKNOWN);

			verify(userRepository).findByEmail(UNKNOWN);
		}
	}
}