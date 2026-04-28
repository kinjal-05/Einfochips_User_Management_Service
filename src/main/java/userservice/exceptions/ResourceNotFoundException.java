package userservice.exceptions;

/**
 * Exception thrown when a requested resource cannot be found in the system.
 *
 * <p>
 * This exception is commonly used when an operation references an entity that
 * does not exist in the database or is unavailable for retrieval.
 *
 * <p>
 * Typical use cases include:
 * <ul>
 * <li>Fetching a user by an invalid or non-existent ID</li>
 * <li>Searching for a record that has been deleted</li>
 * <li>Updating or deleting a resource that does not exist</li>
 * </ul>
 *
 * <p>
 * This is an unchecked exception extending {@link RuntimeException}, allowing
 * it to propagate to the global exception handler layer (e.g.,
 * {@code @ControllerAdvice}) for centralized error handling.
 *
 * <p>
 * Recommended HTTP mapping:
 * <ul>
 * <li><b>404 Not Found</b> – when the requested resource does not exist</li>
 * </ul>
 *
 * @author Kinjal Mistry
 * @version 1.0
 * @since 1.0
 */
public class ResourceNotFoundException extends RuntimeException {

	/**
	 * Constructs a new exception with the specified detail message.
	 *
	 * @param message detailed explanation describing the missing resource
	 */
	public ResourceNotFoundException(String message) {
		super(message);
	}
}