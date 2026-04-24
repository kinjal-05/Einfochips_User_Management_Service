package userservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import userservice.dtos.ChangePasswordRequestDTO;
import userservice.exceptions.ResourceNotFoundException;
import userservice.models.User;
import userservice.repositories.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChangePasswordServiceImpl implements ChangePasswordService{
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	/**
	 * Changes the password of the currently authenticated user.
	 *
	 * Transactional Behavior:
	 * - Uses READ_COMMITTED isolation to avoid dirty reads.
	 * - Rolls back for any exception to maintain data consistency.
	 *
	 * Flow:
	 * - Fetch authenticated user from security context.
	 * - Validate old password.
	 * - Encode and update new password.
	 *
	 * Security:
	 * - Prevents password update if old password is incorrect.
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void changePassword(ChangePasswordRequestDTO request) {
		String loggedInEmail = getCurrentUserEmail();
		User user = userRepository.findByEmail(loggedInEmail)
				.orElseThrow(() -> new ResourceNotFoundException(
						"User not found with email: " + loggedInEmail));

		if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
			throw new BadCredentialsException("Old password is incorrect");
		}
		user.setPassword(passwordEncoder.encode(request.newPassword()));

		userRepository.save(user);
		log.info("Coming After Save in Repo");
	}

	/**
	 * Retrieves the currently authenticated user's email from SecurityContext.
	 *
	 * Security:
	 * - Ensures user is authenticated before accessing context.
	 * - Throws exception for anonymous or unauthenticated access.
	 */
	private static String getCurrentUserEmail() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()
				|| auth.getName().equals("anonymousUser")) {
			throw new BadCredentialsException("User is not authenticated");
		}
		return auth.getName();
	}
}
