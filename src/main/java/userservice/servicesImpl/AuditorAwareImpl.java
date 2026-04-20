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
	public Optional<Long> getCurrentAuditor()
	{
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated() && authentication.getName().equals("anonymousUser")) {
				return Optional.of(0L);
		}
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		return Optional.of(userDetails.getUser().getId());
	}
}
