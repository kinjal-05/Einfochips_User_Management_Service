package userservice.enums;

/**
 * Defines the supported authorization roles within the User Service.
 *
 * <p>
 * This enum is used for role-based access control (RBAC) across the application
 * to determine user permissions and access levels.
 *
 * <p>
 * Typical usage includes:
 * <ul>
 * <li>Assigning roles during user registration or administration</li>
 * <li>Validating access to secured REST endpoints</li>
 * <li>Integrating with Spring Security authorities</li>
 * </ul>
 *
 * <p>
 * Each enum constant follows the standard Spring Security naming convention
 * using the {@code ROLE_} prefix.
 *
 * @author Kinjal Mistry
 * @version 1.0
 * @since 1.0
 */
public enum Role {

	/**
	 * Administrator role with elevated privileges.
	 *
	 * <p>
	 * Users assigned this role typically have permission to manage users, system
	 * settings, reports, and other protected resources.
	 */
	ROLE_ADMIN,

	/**
	 * Standard user role with regular access permissions.
	 *
	 * <p>
	 * Users assigned this role typically have access to their own profile, personal
	 * data, and user-level application features.
	 */
	ROLE_USER
}
