package userservice.services.impls;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import userservice.security.CustomUserDetails;

/**
 * Implementation of {@link AuditorAware} used for Spring Data JPA auditing.
 *
 * <p>
 * This component provides the currently authenticated user's identifier so that
 * audit fields such as:
 * <ul>
 * <li>{@code createdBy}</li>
 * <li>{@code updatedBy}</li>
 * </ul>
 * can be automatically populated during database persistence operations.
 *
 * <p>
 * The auditor value is extracted from the active Spring Security context.
 *
 * <p>
 * If no authenticated user is available (anonymous request, unauthenticated
 * request, or unsupported principal type), an empty result is returned.
 *
 * <p>
 * This bean is registered with the name {@code auditorAwareImpl} and is
 * typically referenced in
 * {@code @EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")}.
 *
 * @author Kinjal Mistry
 * @version 1.0
 * @since 1.0
 */
@Component("auditorAwareImpl")
public class AuditorAwareImpl implements AuditorAware<Long> {

	/**
	 * Returns the current authenticated user's ID for auditing purposes.
	 *
	 * <p>
	 * Resolution flow:
	 * <ul>
	 * <li>Fetch current authentication from SecurityContext</li>
	 * <li>Ensure request is authenticated and not anonymous</li>
	 * <li>Extract principal object</li>
	 * <li>Return user ID when principal is {@link CustomUserDetails}</li>
	 * </ul>
	 *
	 * <p>
	 * If no valid authenticated user exists, returns {@link Optional#empty()}.
	 *
	 * @return authenticated user ID wrapped in Optional, or empty if unavailable
	 */
	@Override
	public Optional<Long> getCurrentAuditor() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getPrincipal())) {
			return Optional.empty();
		}

		Object principal = authentication.getPrincipal();

		if (principal instanceof CustomUserDetails userDetails) {
			return Optional.of(userDetails.getUser().getId());
		}

		return Optional.empty();
	}
}