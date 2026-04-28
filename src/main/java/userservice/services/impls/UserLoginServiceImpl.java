package userservice.services.impls;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import userservice.config.BaseLogger;
import userservice.dtos.UserLoginRequestDTO;
import userservice.dtos.UserLoginResponseDTO;
import userservice.models.User;
import userservice.security.CustomUserDetails;
import userservice.security.JwtService;
import userservice.services.UserLoginService;

@Service
@RequiredArgsConstructor
public class UserLoginServiceImpl extends BaseLogger implements UserLoginService {
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;

	/**
	 * Authenticates user credentials and generates JWT token.
	 *
	 * Flow: - Delegates authentication to AuthenticationManager. - On success,
	 * extracts authenticated user details. - Generates JWT token for stateless
	 * authentication.
	 *
	 * Security: - Throws BadCredentialsException if authentication fails. - No
	 * transaction required (read + auth operation only).
	 */
	@Override
	@Transactional
	public UserLoginResponseDTO login(UserLoginRequestDTO request) {
		Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
		CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
		String token = jwtService.generateToken(customUserDetails);
		User user = customUserDetails.getUser();
		log.info("{}", user);
		return new UserLoginResponseDTO(user.getId(), user.getEmail(), user.getRole(), token, "Login Successful");
	}
}
