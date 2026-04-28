package userservice.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import userservice.enums.Role;
import userservice.models.User;

/**
 * Specification class for building dynamic database queries for User entity.
 *
 * <p>
 * This class uses Spring Data JPA Specifications to construct flexible and
 * reusable filtering logic based on optional parameters.
 * </p>
 *
 * <p>
 * Benefits: - Avoids writing multiple query methods - Supports dynamic
 * filtering - Improves code maintainability and readability
 * </p>
 */

public class UserSpecification extends BaseLogger {

	private UserSpecification() {
	}

	/**
	 * Builds a dynamic filter query based on provided parameters.
	 *
	 * <p>
	 * Only non-null parameters are included in the query. This allows flexible
	 * search functionality without creating multiple APIs.
	 * </p>
	 *
	 * @param email       Partial or full email (case-insensitive search)
	 * @param role        User role (e.g., ADMIN, USER)
	 * @param createdById ID of user who created the record
	 * @param updatedById ID of user who last updated the record
	 * @param fromDate    Start date for filtering (createdAt >= fromDate)
	 * @param toDate      End date for filtering (createdAt <= toDate)
	 *
	 * @return Specification<User> dynamic query specification
	 */
	public static Specification<User> filterUsers(String email, Role role, Long createdById, Long updatedById,
			LocalDateTime fromDate, LocalDateTime toDate) {

		return (root, query, cb) -> {

			// List to hold dynamic query conditions
			List<Predicate> predicates = new ArrayList<>();

			/**
			 * Filter by email (case-insensitive, partial match)
			 *
			 * Example: Input: "kinjal" Matches: kinjal@gmail.com, testkinjal@yahoo.com
			 */

			// Always exclude soft-deleted users
			predicates.add(cb.isFalse(root.get("isDeleted")));
			if (email != null && !email.trim().isEmpty()) {
				predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase().trim() + "%"));
			}

			/**
			 * Filter by user role
			 *
			 * Example: ADMIN, USER, MANAGER
			 */
			if (role != null) {
				predicates.add(cb.equal(root.get("role"), role));
			}

			/**
			 * Filter by creator ID
			 *
			 * Useful for auditing or multi-user systems
			 */
			if (createdById != null) {
				predicates.add(cb.equal(root.get("createdById"), createdById));
			}

			/**
			 * Filter by last updater ID
			 */
			if (updatedById != null) {
				predicates.add(cb.equal(root.get("updatedById"), updatedById));
			}

			/**
			 * Filter records created after or equal to a specific date
			 */
			if (fromDate != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
			}

			/**
			 * Filter records created before or equal to a specific date
			 */
			if (toDate != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
			}

			/**
			 * Combine all predicates using AND condition
			 *
			 * If no filters are provided, this returns all records
			 */
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}