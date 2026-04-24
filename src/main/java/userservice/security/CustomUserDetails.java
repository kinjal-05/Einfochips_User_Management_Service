package userservice.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import userservice.models.User;
import java.util.Collection;
import java.util.List;


/**
 * Custom implementation of {@link UserDetails} used by Spring Security
 * for authentication and authorization.
 *
 * <p>This class acts as an adapter between the application's {@link User} entity
 * and Spring Security's internal user representation. It provides user-specific
 * data required during the authentication process.
 *
 * <p><b>Key Responsibilities:</b>
 * <ul>
 *   <li><b>User Wrapping:</b> Encapsulates the {@link User} entity and exposes
 *       its properties in a format compatible with Spring Security.</li>
 *
 *   <li><b>Authority Mapping:</b> Converts the user's {@code Role} into a
 *       {@link GrantedAuthority} using the standard {@code ROLE_} prefix
 *       (e.g., {@code ROLE_ADMIN}, {@code ROLE_USER}).</li>
 *
 *   <li><b>Authentication Data:</b>
 *       <ul>
 *         <li>{@code getUsername()} returns the user's email</li>
 *         <li>{@code getPassword()} returns the encrypted password</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Account Status:</b>
 *       <ul>
 *         <li>All account status checks return {@code true} by default
 *             (non-expired, non-locked, credentials valid)</li>
 *         <li>{@code isEnabled()} returns {@code false} if the user is soft-deleted</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>JWT Integration:</b>
 *       The underlying {@link User} object can be accessed via {@code getUser()}
 *       for generating JWT tokens or retrieving additional user details.</li>
 * </ul>
 *
 * <p><b>Security Notes:</b>
 * <ul>
 *   <li>Authorities follow Spring Security conventions using {@code ROLE_} prefix</li>
 *   <li>Password is expected to be stored in encoded (hashed) format</li>
 *   <li>Soft-deleted users are prevented from authenticating via {@code isEnabled()}</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * Typically used by a custom {@code UserDetailsService} implementation to load
 * user-specific data during authentication.
 */
public class CustomUserDetails implements UserDetails {

	@Getter
	private final User user;  // getUser() used by JwtService

	public CustomUserDetails(User user) {
		this.user = user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getUsername() {
		return user.getEmail();
	}

	@Override
	public boolean isAccountNonExpired() { return true; }

	@Override
	public boolean isAccountNonLocked() { return true; }

	@Override
	public boolean isCredentialsNonExpired() { return true; }

	@Override
	public boolean isEnabled() {
		return !user.isDeleted();  // works now because field is named `deleted`
	}
}