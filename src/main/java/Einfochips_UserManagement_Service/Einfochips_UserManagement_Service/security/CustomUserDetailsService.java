package Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import Einfochips_UserManagement_Service.Einfochips_UserManagement_Service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return userRepository.findByEmail(email).map(CustomUserDetails::new)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with email"));
	}

}
