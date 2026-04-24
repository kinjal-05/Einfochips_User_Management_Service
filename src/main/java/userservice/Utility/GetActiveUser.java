package userservice.Utility;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import userservice.exceptions.ResourceNotFoundException;
import userservice.models.User;
import userservice.repositories.UserRepository;

/**
 * Utility component for retrieving active (non-deleted) users from the database.
 *
 * <p>This class provides a reusable method to fetch a user by ID while ensuring
 * that only active (non-soft-deleted) users are returned. It centralizes the
 * logic for validating user existence and active status across the application.
 *
 * <p><b>Key Responsibilities:</b>
 * <ul>
 *   <li><b>Active User Retrieval:</b>
 *       Fetches a user using {@code findActiveById}, which excludes soft-deleted users.</li>
 *
 *   <li><b>Exception Handling:</b>
 *       Throws {@link ResourceNotFoundException} if the user does not exist
 *       or is marked as deleted.</li>
 *
 *   <li><b>Code Reusability:</b>
 *       Eliminates repetitive null checks and existence validation logic
 *       in service or controller layers.</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>
 *     User user = getActiveUser.getUserOrThrow(userId);
 * </pre>
 *
 * <p><b>Design Notes:</b>
 * <ul>
 *   <li>Encapsulates repository access for cleaner service layer code</li>
 *   <li>Ensures consistent error handling across the application</li>
 *   <li>Works with soft delete strategy (i.e., {@code isDeleted = false})</li>
 * </ul>
 *
 * @throws ResourceNotFoundException if no active user is found with the given ID
 */
@RequiredArgsConstructor
@Getter
@Component
public class GetActiveUser {

	private final UserRepository userRepository;

	public User getUserOrThrow(long id) {
		return userRepository.findActiveById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
	}
}
