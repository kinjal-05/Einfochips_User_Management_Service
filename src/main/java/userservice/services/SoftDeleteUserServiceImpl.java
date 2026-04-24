package userservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import userservice.Utility.GetActiveUser;
import userservice.exceptions.ResourceNotFoundException;
import userservice.models.User;
import userservice.repositories.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class SoftDeleteUserServiceImpl implements  SoftDeleteUserService{
	private final UserRepository userRepository;
	private final GetActiveUser getActiveUser;
	/**
	 * Performs soft delete on a user.
	 *
	 * Transactional Behavior:
	 * - Wrapped in transaction to ensure delete operation consistency.
	 *
	 * Notes:
	 * - Uses @SQLDelete (Hibernate) to mark user as deleted instead of physical deletion.
	 */
	@Override
	@Transactional
	public void softDeleteUser(long id) {
		User user = getActiveUser.getUserOrThrow(id);
		userRepository.delete(user);
	}

}
