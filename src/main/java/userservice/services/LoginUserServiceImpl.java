package userservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import userservice.dtos.LoginRequestDTO;
import userservice.dtos.LoginResponseDTO;
import userservice.models.User;
import userservice.security.CustomUserDetails;
import userservice.security.JwtService;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoginUserServiceImpl implements LoginUserService{
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	/**
	 * Authenticates user credentials and generates JWT token.
	 *
	 * Flow:
	 * - Delegates authentication to AuthenticationManager.
	 * - On success, extracts authenticated user details.
	 * - Generates JWT token for stateless authentication.
	 *
	 * Security:
	 * - Throws BadCredentialsException if authentication fails.
	 * - No transaction required (read + auth operation only).
	 */
	@Override
	@Transactional
	public LoginResponseDTO login(LoginRequestDTO request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						request.email(),
						request.password()
				)
		);
		CustomUserDetails customUserDetails =
				(CustomUserDetails) authentication.getPrincipal();
		String token = jwtService.generateToken(customUserDetails);
		User user = customUserDetails.getUser();
		log.info("{}",user);
		return new LoginResponseDTO(
				user.getId(),
				user.getEmail(),
				user.getRole(),
				token,
				"Login Successful"
		);
	}
}
