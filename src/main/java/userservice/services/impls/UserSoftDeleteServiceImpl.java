package userservice.services.impls;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import userservice.models.User;
import userservice.repositories.UserRepository;
import userservice.services.UserSoftDeleteService;
import userservice.utility.GetActiveUser;

/**
 * Service implementation responsible for performing soft delete operations on
 * user accounts.
 *
 * <p>
 * This service removes users logically rather than physically deleting records
 * from the database.
 *
 * <p>
 * Soft delete behavior is typically implemented using Hibernate annotations
 * such as:
 * <ul>
 * <li>{@code @SQLDelete}</li>
 * <li>{@code @Where}</li>
 * </ul>
 *
 * <p>
 * Instead of removing the database row, the user record is marked as deleted
 * (for example: {@code isDeleted = true}), preserving historical and audit
 * data.
 *
 * <p>
 * This approach is useful for:
 * <ul>
 * <li>Audit compliance</li>
 * <li>Data recovery</li>
 * <li>Maintaining relational integrity</li>
 * <li>Tracking user lifecycle history</li>
 * </ul>
 *
 * <p>
 * The delete operation is executed within a transactional boundary to ensure
 * consistency.
 *
 * @author Kinjal Mistry
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class UserSoftDeleteServiceImpl implements UserSoftDeleteService {
	private final UserRepository userRepository;
	private final GetActiveUser getActiveUser;

	/**
	 * Performs a soft delete on the specified user.
	 *
	 * <p>
	 * Execution flow:
	 * <ul>
	 * <li>Retrieve active user by ID</li>
	 * <li>Throw exception if user does not exist or already deleted</li>
	 * <li>Invoke repository delete method</li>
	 * <li>Hibernate converts delete into logical update</li>
	 * </ul>
	 *
	 * <p>
	 * No physical database row removal occurs when soft delete is properly
	 * configured.
	 *
	 * @param id unique identifier of the user to delete
	 */
	@Override
	@Transactional
	public void softDeleteUser(long id) {
		User user = getActiveUser.getUserOrThrow(id);
		userRepository.delete(user);
	}

}
