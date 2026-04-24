package userservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import userservice.models.User;
import userservice.repositories.UserRepository;
import java.util.Optional;

/**
 * Custom implementation of {@link UserDetailsService} used by Spring Security
 * to load user-specific data during authentication.
 *
 * <p>This service retrieves user information from the database using the
 * {@link UserRepository} and converts it into a {@link CustomUserDetails}
 * object, which is then used by Spring Security for authentication and authorization.
 *
 * <p><b>Key Responsibilities:</b>
 * <ul>
 *   <li><b>User Lookup:</b> Fetches a {@link User} entity based on the provided email.</li>
 *
 *   <li><b>Authentication Integration:</b> Converts the {@link User} entity into
 *       a {@link CustomUserDetails} instance compatible with Spring Security.</li>
 *
 *   <li><b>Exception Handling:</b> Throws {@link UsernameNotFoundException} if
 *       no user is found for the given email, which is required by Spring Security
 *       to handle authentication failures properly.</li>
 * </ul>
 *
 * <p><b>Authentication Flow:</b>
 * <ol>
 *   <li>Spring Security calls {@code loadUserByUsername(email)} during login</li>
 *   <li>User is fetched from the database using {@code userRepository}</li>
 *   <li>If user is not found, {@code UsernameNotFoundException} is thrown</li>
 *   <li>If found, user is wrapped inside {@link CustomUserDetails}</li>
 *   <li>Returned object is used by AuthenticationManager for validation</li>
 * </ol>
 *
 * <p><b>Security Notes:</b>
 * <ul>
 *   <li>Email is used as the username for authentication</li>
 *   <li>Returned {@link CustomUserDetails} contains encoded password and authorities</li>
 *   <li>Exception message should avoid exposing sensitive information in production logs</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * Registered as a Spring {@code @Service} and automatically picked up by
 * Spring Security configuration during authentication setup.
 *
 * @throws UsernameNotFoundException if no user exists with the given email
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;  //ONLY this field here

	@Override
	public UserDetails loadUserByUsername(String email)
			throws UsernameNotFoundException {

		Optional<User> result = userRepository.findByEmail(email);

		if (result.isEmpty()) {
			throw new UsernameNotFoundException(
					"User not found with email: " + email);
		}

		User user = result.get();
		return new CustomUserDetails(user);
	}
}