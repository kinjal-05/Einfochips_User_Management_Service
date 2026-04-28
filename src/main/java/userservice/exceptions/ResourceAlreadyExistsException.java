package userservice.exceptions;

/**
 * Exception thrown when attempting to create or register a resource that
 * already exists in the system.
 *
 * <p>
 * This exception is typically used in scenarios where uniqueness constraints
 * are violated, such as:
 * <ul>
 * <li>Registering a user with an existing email address</li>
 * <li>Creating an entity with a duplicate unique identifier</li>
 * <li>Adding a resource that already exists in the database</li>
 * </ul>
 *
 * <p>
 * This is an unchecked exception extending {@link RuntimeException}, allowing
 * it to propagate through the application and be handled by a global exception
 * handler (e.g., {@code @ControllerAdvice}).
 *
 * <p>
 * Recommended HTTP mapping:
 * <ul>
 * <li><b>409 Conflict</b> – when the resource already exists</li>
 * </ul>
 *
 * @author Kinjal Mistry
 * @version 1.0
 * @since 1.0
 */
public class ResourceAlreadyExistsException extends RuntimeException {

	/**
	 * Constructs a new exception with the specified detail message.
	 *
	 * @param message detailed explanation of the conflict or duplicate resource
	 */
	public ResourceAlreadyExistsException(String message) {
		super(message);
	}
}
