package userservice.servicesImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.security.CustomUserDetails;

import java.util.Optional;

/**
 * Implementation of {@link AuditorAware} used by Spring Data JPA Auditing.
 *
 * Purpose:
 * - Provides the current user's ID for audit fields like:
 *   - createdBy
 *   - updatedBy
 *
 * How it works:
 * - Fetches the current Authentication from SecurityContext.
 * - Extracts the authenticated user's ID from CustomUserDetails.
 *
 * Usage:
 * - Automatically used when @CreatedBy / @LastModifiedBy annotations are present
 *   in entity classes.
 *
 * Example:
 *   @CreatedBy
 *   private Long createdById;
 *
 *   @LastModifiedBy
 *   private Long updatedById;
 */
@Component("auditorAwareImpl")
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<Long> {
	private final UserRepository userRepository;

	/**
	 * Returns the currently authenticated user's ID.
	 *
	 * Flow:
	 * - Retrieve Authentication from SecurityContextHolder.
	 * - Validate authentication state.
	 * - Extract user ID from CustomUserDetails.
	 *
	 * Edge Cases Handled:
	 * - No authentication present → return empty
	 * - Unauthenticated request → return empty
	 * - Anonymous user → return default ID (0L)
	 *
	 * Why Optional:
	 * - Allows auditing framework to handle absence of user gracefully.
	 *
	 * @return Optional containing user ID or empty if unavailable
	 */
	@Override
	public Optional<Long> getCurrentAuditor() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth == null) return Optional.empty();
		if (!auth.isAuthenticated()) return Optional.empty();

		if ("anonymousUser".equals(auth.getName())) return Optional.of(0L);

		return (auth.getPrincipal() instanceof CustomUserDetails userDetails)
				? Optional.of(userDetails.getUser().getId())
				: Optional.empty();
	}


}
