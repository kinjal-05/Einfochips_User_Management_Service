package userservice.dtos;

/**
 * Projection interface for fetching partial User data.
 *
 * <p>
 * This interface is used with Spring Data JPA to retrieve only
 * specific fields (email and role) instead of the full User entity.
 * </p>
 *
 * <p>
 * Benefits:
 * - Improves performance (fetches only required columns)
 * - Reduces memory usage
 * - Faster query execution
 * </p>
 *
 * <p>
 * Used with custom JPQL query:
 * SELECT u.email AS email, u.role AS role FROM User u
 * </p>
 *
 * <p>
 * NOTE:
 * - Method names must match the alias used in the query
 * - Spring automatically maps query results to this interface
 * </p>
 */
public interface UserEmailRoleProjection {

	/**
	 * Gets the user's email.
	 *
	 * @return email
	 */
	String getEmail();

	/**
	 * Gets the user's role.
	 *
	 * <p>
	 * NOTE:
	 * - Returned as String because projection maps raw query result
	 * - Can be converted to enum (Role) if needed in service layer
	 * </p>
	 *
	 * @return role as String
	 */
	String getRole();
}