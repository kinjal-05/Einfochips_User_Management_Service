package userservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import userservice.models.User;
import userservice.repositories.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;  // ← ONLY this field here

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