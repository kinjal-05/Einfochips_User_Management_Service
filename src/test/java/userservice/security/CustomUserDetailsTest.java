package userservice.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import userservice.enums.Role;
import userservice.models.User;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CustomUserDetails}
 *
 * No Mockito needed — User is a simple POJO, so we use real instances.
 *
 * Branches covered:
 *  1.  getAuthorities()           — returns "ROLE_" + role.name()
 *  2.  getPassword()              — delegates to user.getPassword()
 *  3.  getUsername()              — delegates to user.getEmail()
 *  4.  isAccountNonExpired()      — always true
 *  5.  isAccountNonLocked()       — always true
 *  6.  isCredentialsNonExpired()  — always true
 *  7.  isEnabled()                — true  when user.isDeleted() = false
 *  8.  isEnabled()                — false when user.isDeleted() = true
 *  9.  getUser()  (@Getter)       — returns the wrapped User
 */
@ActiveProfiles("test")
class CustomUserDetailsTest {

	// ── helper ────────────────────────────────────────────────────────────────

	private User buildUser(String email,
	                       String password,
	                       Role role,
	                       boolean deleted) {
		User user = new User();
		user.setEmail(email);
		user.setPassword(password);
		user.setRole(role);
		user.setDeleted(deleted);
		return user;
	}

	// =========================================================================
	// getUser() — @Getter
	// =========================================================================
	@Nested
	@DisplayName("getUser")
	class GetUserTests {

		@Test
		@DisplayName("returns the exact User instance passed to constructor")
		void returnsWrappedUser() {
			User user = buildUser("a@example.com", "pass", Role.ROLE_USER, false);
			CustomUserDetails details = new CustomUserDetails(user);

			assertThat(details.getUser()).isSameAs(user);
		}
	}

	// =========================================================================
	// getAuthorities()
	// =========================================================================
	@Nested
	@DisplayName("getAuthorities")
	class GetAuthoritiesTests {

		@Test
		@DisplayName("ROLE_USER → authority is 'ROLE_ROLE_USER'")
		void roleUser_correctAuthority() {
			User user = buildUser("a@example.com", "pass", Role.ROLE_USER, false);
			CustomUserDetails details = new CustomUserDetails(user);

			Collection<? extends GrantedAuthority> authorities = details.getAuthorities();

			assertThat(authorities)
					.hasSize(1)
					.extracting(GrantedAuthority::getAuthority)
					.containsExactly("ROLE_" + Role.ROLE_USER.name());
		}

		@Test
		@DisplayName("ROLE_ADMIN → authority is 'ROLE_ROLE_ADMIN'")
		void roleAdmin_correctAuthority() {
			User user = buildUser("admin@example.com", "pass", Role.ROLE_ADMIN, false);
			CustomUserDetails details = new CustomUserDetails(user);

			Collection<? extends GrantedAuthority> authorities = details.getAuthorities();

			assertThat(authorities)
					.hasSize(1)
					.extracting(GrantedAuthority::getAuthority)
					.containsExactly("ROLE_" + Role.ROLE_ADMIN.name());
		}
	}

	// =========================================================================
	// getPassword()
	// =========================================================================
	@Nested
	@DisplayName("getPassword")
	class GetPasswordTests {

		@Test
		@DisplayName("returns password from the wrapped User")
		void returnsUserPassword() {
			User user = buildUser("a@example.com", "secret123", Role.ROLE_USER, false);
			CustomUserDetails details = new CustomUserDetails(user);

			assertThat(details.getPassword()).isEqualTo("secret123");
		}

		@Test
		@DisplayName("returns null when user password is null")
		void returnsNullWhenPasswordIsNull() {
			User user = buildUser("a@example.com", null, Role.ROLE_USER, false);
			CustomUserDetails details = new CustomUserDetails(user);

			assertThat(details.getPassword()).isNull();
		}
	}

	// =========================================================================
	// getUsername()
	// =========================================================================
	@Nested
	@DisplayName("getUsername")
	class GetUsernameTests {

		@Test
		@DisplayName("returns email from the wrapped User")
		void returnsUserEmail() {
			User user = buildUser("kinjal@example.com", "pass", Role.ROLE_USER, false);
			CustomUserDetails details = new CustomUserDetails(user);

			assertThat(details.getUsername()).isEqualTo("kinjal@example.com");
		}
	}

	// =========================================================================
	// isAccountNonExpired / isAccountNonLocked / isCredentialsNonExpired
	// — all hardcoded true
	// =========================================================================
	@Nested
	@DisplayName("hardcoded-true flags")
	class HardcodedTrueFlagsTests {

		private final CustomUserDetails details =
				new CustomUserDetails(
						buildUser("a@example.com", "pass", Role.ROLE_USER, false));

		@Test
		@DisplayName("isAccountNonExpired() always returns true")
		void isAccountNonExpired_alwaysTrue() {
			assertThat(details.isAccountNonExpired()).isTrue();
		}

		@Test
		@DisplayName("isAccountNonLocked() always returns true")
		void isAccountNonLocked_alwaysTrue() {
			assertThat(details.isAccountNonLocked()).isTrue();
		}

		@Test
		@DisplayName("isCredentialsNonExpired() always returns true")
		void isCredentialsNonExpired_alwaysTrue() {
			assertThat(details.isCredentialsNonExpired()).isTrue();
		}
	}

	// =========================================================================
	// isEnabled() — the key branch: !user.isDeleted()
	// =========================================================================
	@Nested
	@DisplayName("isEnabled")
	class IsEnabledTests {

		@Test
		@DisplayName("returns true when user is NOT deleted (deleted = false)")
		void notDeleted_returnsTrue() {
			User user = buildUser("active@example.com", "pass", Role.ROLE_USER, false);
			CustomUserDetails details = new CustomUserDetails(user);

			assertThat(details.isEnabled()).isTrue();
		}

		@Test
		@DisplayName("returns false when user IS deleted (deleted = true)")
		void deleted_returnsFalse() {
			User user = buildUser("deleted@example.com", "pass", Role.ROLE_USER, true);
			CustomUserDetails details = new CustomUserDetails(user);

			assertThat(details.isEnabled()).isFalse();
		}
	}
}