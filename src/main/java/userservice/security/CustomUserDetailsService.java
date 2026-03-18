package userservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import userservice.repositories.UserRepository;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email)
			throws UsernameNotFoundException {

		log.info("=== loadUserByUsername() called with email: '{}'", email);
		log.info("=== Email length: {}", email != null ? email.length() : "null");
		log.info("=== Email bytes: {}", email != null ? java.util.Arrays.toString(email.getBytes()) : "null");

		// ── Step 1: call repository and log result immediately ──────────────
		Optional<userservice.models.User> result = userRepository.findByEmail(email);

		log.info("=== findByEmail() returned isPresent: {}", result.isPresent());

		if (result.isEmpty()) {
			// ── Step 2: try trimmed email to detect whitespace issue ─────────
			String trimmedEmail = email.trim();
			log.warn("=== Retrying with trimmed email: '{}'", trimmedEmail);

			Optional<userservice.models.User> retryResult =
					userRepository.findByEmail(trimmedEmail);

			log.warn("=== Retry result isPresent: {}", retryResult.isPresent());

			// ── Step 3: count total users in DB ─────────────────────────────
			long count = userRepository.count();
			log.warn("=== Total users in DB: {}", count);

			// ── Step 4: list all emails in DB ───────────────────────────────
			userRepository.findAll().forEach(u ->
					log.warn("=== DB has user: id={}, email='{}', isDeleted={}",
							u.getId(), u.getEmail(), u.isDeleted())
			);

			log.error("=== User NOT FOUND for email: '{}'", email);
			throw new UsernameNotFoundException(
					"User not found with email: " + email);
		}

		// ── Step 5: user found — log all details ────────────────────────────
		userservice.models.User user = result.get();

		log.info("=== User FOUND: email='{}'", user.getEmail());
		log.info("=== isDeleted : {}", user.isDeleted());
		log.info("=== role      : {}", user.getRole());
		log.info("=== password is BCrypt: {}",
				user.getPassword() != null && user.getPassword().startsWith("$2a"));

		CustomUserDetails details = new CustomUserDetails(user);

		log.info("=== getUsername()       : {}", details.getUsername());
		log.info("=== isEnabled()         : {}", details.isEnabled());
		log.info("=== isAccountNonLocked(): {}", details.isAccountNonLocked());
		log.info("=== getAuthorities()    : {}", details.getAuthorities());

		return details;
	}
}