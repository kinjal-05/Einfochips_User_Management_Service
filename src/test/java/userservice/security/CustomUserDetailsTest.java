package userservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import userservice.enums.Role;
import userservice.models.User;

/**
 * Unit tests for {@link CustomUserDetails}
 *
 * No Mockito needed — User is a simple POJO, so we use real instances.
 *
 * Branches covered: 1. getAuthorities() — returns "ROLE_" + role.name() 2.
 * getPassword() — delegates to user.getPassword() 3. getUsername() — delegates
 * to user.getEmail() 4. isAccountNonExpired() — always true 5.
 * isAccountNonLocked() — always true 6. isCredentialsNonExpired() — always true
 * 7. isEnabled() — true when user.isDeleted() = false 8. isEnabled() — false
 * when user.isDeleted() = true 9. getUser() (@Getter) — returns the wrapped
 * User
 */
@ActiveProfiles("test")
class CustomUserDetailsTest {

	// ── helper ────────────────────────────────────────────────────────────────

	private User buildUser(String email, String password, Role role, boolean deleted) {
		User user = new User();
		user.setEmail(email);
		user.setPassword(password);
		user.setRole(role);
		user.setDeleted(deleted);
		return user;
	}

	/**
	 * Test suite validating access to the wrapped domain user inside
	 * {@link CustomUserDetails}.
	 *
	 * <p>
	 * Ensures the security wrapper preserves the original {@link User} reference
	 * passed during construction.
	 */
	@Nested
	@DisplayName("getUser")
	class GetUserTests {

		/**
		 * Verifies that {@code getUser()} returns the exact same {@link User} instance
		 * provided to the constructor.
		 *
		 * <p>
		 * Expected behavior:
		 * <ul>
		 * <li>No defensive copy is created</li>
		 * <li>Returned reference is identical to original object</li>
		 * </ul>
		 */
		@Test
		@DisplayName("returns the exact User instance passed to constructor")
		void returnsWrappedUser() {
			User user = buildUser("a@example.com", "pass", Role.ROLE_USER, false);
			CustomUserDetails details = new CustomUserDetails(user);

			assertThat(details.getUser()).isSameAs(user);
		}
	}

	/**
	 * Test suite validating Spring Security authority generation from application
	 * roles.
	 *
	 * <p>
	 * Ensures each stored role is converted into the expected
	 * {@link GrantedAuthority} collection.
	 */
	@Nested
	@DisplayName("getAuthorities")
	class GetAuthoritiesTests {

		/**
		 * Verifies authority generation for {@code ROLE_USER}.
		 *
		 * <p>
		 * Expected authority: {@code ROLE_ROLE_USER}
		 */
		@Test
		@DisplayName("ROLE_USER → authority is 'ROLE_ROLE_USER'")
		void roleUser_correctAuthority() {
			User user = buildUser("a@example.com", "pass", Role.ROLE_USER, false);
			CustomUserDetails details = new CustomUserDetails(user);

			Collection<? extends GrantedAuthority> authorities = details.getAuthorities();

			assertThat(authorities).hasSize(1).extracting(GrantedAuthority::getAuthority)
					.containsExactly("ROLE_" + Role.ROLE_USER.name());
		}

		/**
		 * Verifies authority generation for {@code ROLE_ADMIN}.
		 *
		 * <p>
		 * Expected authority: {@code ROLE_ROLE_ADMIN}
		 */
		@Test
		@DisplayName("ROLE_ADMIN → authority is 'ROLE_ROLE_ADMIN'")
		void roleAdmin_correctAuthority() {
			User user = buildUser("admin@example.com", "pass", Role.ROLE_ADMIN, false);
			CustomUserDetails details = new CustomUserDetails(user);

			Collection<? extends GrantedAuthority> authorities = details.getAuthorities();

			assertThat(authorities).hasSize(1).extracting(GrantedAuthority::getAuthority)
					.containsExactly("ROLE_" + Role.ROLE_ADMIN.name());
		}
	}

	/**
	 * Test suite validating password exposure behavior in
	 * {@link CustomUserDetails}.
	 *
	 * <p>
	 * Ensures the security wrapper delegates password access directly to the
	 * underlying {@link User} entity.
	 */
	@Nested
	@DisplayName("getPassword")
	class GetPasswordTests {

		/**
		 * Verifies that {@code getPassword()} returns the exact password stored in the
		 * wrapped user object.
		 *
		 * <p>
		 * Expected behavior:
		 * <ul>
		 * <li>Password value is delegated unchanged</li>
		 * <li>Supports authentication comparison logic</li>
		 * </ul>
		 */
		@Test
		@DisplayName("returns password from the wrapped User")
		void returnsUserPassword() {
			User user = buildUser("a@example.com", "secret123", Role.ROLE_USER, false);
			CustomUserDetails details = new CustomUserDetails(user);

			assertThat(details.getPassword()).isEqualTo("secret123");
		}

		/**
		 * Verifies that {@code getPassword()} safely returns null when the wrapped user
		 * has no password assigned.
		 *
		 * <p>
		 * Expected behavior:
		 * <ul>
		 * <li>No exception is thrown</li>
		 * <li>Null value is returned transparently</li>
		 * </ul>
		 */
		@Test
		@DisplayName("returns null when user password is null")
		void returnsNullWhenPasswordIsNull() {
			User user = buildUser("a@example.com", null, Role.ROLE_USER, false);
			CustomUserDetails details = new CustomUserDetails(user);

			assertThat(details.getPassword()).isNull();
		}
	}

	/**
	 * Test suite validating username resolution behavior in
	 * {@link CustomUserDetails}.
	 *
	 * <p>
	 * Ensures the application uses user email as the Spring Security username
	 * identifier.
	 */
	@Nested
	@DisplayName("getUsername")
	class GetUsernameTests {

		/**
		 * Verifies that {@code getUsername()} returns the wrapped user's email address.
		 *
		 * <p>
		 * Expected behavior:
		 * <ul>
		 * <li>Email acts as login username</li>
		 * <li>Returned value matches stored email exactly</li>
		 * </ul>
		 */

		@Test
		@DisplayName("returns email from the wrapped User")
		void returnsUserEmail() {
			User user = buildUser("kinjal@example.com", "pass", Role.ROLE_USER, false);
			CustomUserDetails details = new CustomUserDetails(user);

			assertThat(details.getUsername()).isEqualTo("kinjal@example.com");
		}
	}

	/**
	 * Test suite validating account status flags that are intentionally hardcoded
	 * to {@code true}.
	 *
	 * <p>
	 * These methods satisfy Spring Security's
	 * {@link org.springframework.security.core.userdetails.UserDetails} contract
	 * when the application does not currently enforce account expiry, locking, or
	 * credential expiry policies.
	 */
	@Nested
	@DisplayName("hardcoded-true flags")
	class HardcodedTrueFlagsTests {

		/**
		 * Shared test instance representing an active user.
		 */
		private final CustomUserDetails details = new CustomUserDetails(
				buildUser("a@example.com", "pass", Role.ROLE_USER, false));

		/**
		 * Verifies that account expiration is not enforced.
		 *
		 * <p>
		 * Expected behavior: {@code isAccountNonExpired()} always returns {@code true}.
		 */
		@Test
		@DisplayName("isAccountNonExpired() always returns true")
		void isAccountNonExpired_alwaysTrue() {
			assertThat(details.isAccountNonExpired()).isTrue();
		}

		/**
		 * Verifies that account locking is not enforced.
		 *
		 * <p>
		 * Expected behavior: {@code isAccountNonLocked()} always returns {@code true}.
		 */
		@Test
		@DisplayName("isAccountNonLocked() always returns true")
		void isAccountNonLocked_alwaysTrue() {
			assertThat(details.isAccountNonLocked()).isTrue();
		}

		/**
		 * Verifies that credential expiration is not enforced.
		 *
		 * <p>
		 * Expected behavior: {@code isCredentialsNonExpired()} always returns
		 * {@code true}.
		 */
		@Test
		@DisplayName("isCredentialsNonExpired() always returns true")
		void isCredentialsNonExpired_alwaysTrue() {
			assertThat(details.isCredentialsNonExpired()).isTrue();
		}
	}

	/**
	 * Test suite validating account enabled status behavior in
	 * {@link CustomUserDetails}.
	 *
	 * <p>
	 * The enabled flag is derived from the wrapped user's soft-delete state. Active
	 * users remain enabled, while logically deleted users are disabled from
	 * authentication.
	 */
	@Nested
	@DisplayName("isEnabled")
	class IsEnabledTests {

		/**
		 * Verifies that a user marked as not deleted is considered enabled.
		 *
		 * <p>
		 * Expected behavior:
		 * <ul>
		 * <li>{@code isDeleted = false}</li>
		 * <li>{@code isEnabled()} returns {@code true}</li>
		 * </ul>
		 */
		@Test
		@DisplayName("returns true when user is NOT deleted (deleted = false)")
		void notDeleted_returnsTrue() {
			User user = buildUser("active@example.com", "pass", Role.ROLE_USER, false);
			CustomUserDetails details = new CustomUserDetails(user);

			assertThat(details.isEnabled()).isTrue();
		}

		/**
		 * Verifies that a soft-deleted user is considered disabled.
		 *
		 * <p>
		 * Expected behavior:
		 * <ul>
		 * <li>{@code isDeleted = true}</li>
		 * <li>{@code isEnabled()} returns {@code false}</li>
		 * </ul>
		 */
		@Test
		@DisplayName("returns false when user IS deleted (deleted = true)")
		void deleted_returnsFalse() {
			User user = buildUser("deleted@example.com", "pass", Role.ROLE_USER, true);
			CustomUserDetails details = new CustomUserDetails(user);

			assertThat(details.isEnabled()).isFalse();
		}
	}
}