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

@Component("auditorAwareImpl")
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<Long> {
	private final UserRepository userRepository;

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
