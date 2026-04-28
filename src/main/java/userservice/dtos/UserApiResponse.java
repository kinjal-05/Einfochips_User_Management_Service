package userservice.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Generic API response wrapper for standardizing all HTTP responses.
 *
 * <p>
 * This class provides a consistent structure for API responses across
 * the application. It encapsulates the success status, message, payload data,
 * and timestamp of the response.
 * </p>
 *
 * <p>
 * <b>Response Structure:</b>
 * <ul>
 *   <li>{@code success} - Indicates whether the request was successful.</li>
 *   <li>{@code message} - Descriptive message about the response.</li>
 *   <li>{@code data} - Generic payload containing response data (if any).</li>
 *   <li>{@code timestamp} - Time at which the response was generated.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Usage:</b>
 * <ul>
 *   <li>Use {@link #success(Object, String)} for successful responses.</li>
 *   <li>Use {@link #failure(String)} for error responses.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This approach ensures consistency in API design, simplifies client-side
 * parsing, and improves maintainability of the application.
 * </p>
 *
 * @param <T> the type of the response payload
 */
@Getter
@Setter
@Builder
public class UserApiResponse<T> {

	private boolean success;
	private String message;
	private T data;
	private LocalDateTime timestamp;

	/**
	 * Creates a successful API response.
	 *
	 * @param data the response payload
	 * @param message the success message
	 * @param <T> the type of the response payload
	 * @return a populated {@code ApiResponse} instance with success status
	 */
	public static <T> UserApiResponse<T> success(T data, String message) {
		return UserApiResponse.<T>builder()
				.success(true)
				.message(message)
				.data(data)
				.timestamp(LocalDateTime.now())
				.build();
	}

	/**
	 * Creates a failure API response.
	 *
	 * @param message the error message
	 * @param <T> the type of the response payload
	 * @return a populated {@code ApiResponse} instance with failure status
	 */
	public static <T> UserApiResponse<T> failure(String message) {
		return UserApiResponse.<T>builder()
				.success(false)
				.message(message)
				.data(null)
				.timestamp(LocalDateTime.now())
				.build();
	}
}