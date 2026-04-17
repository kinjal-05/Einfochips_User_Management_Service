package userservice.dtos;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Standard error response DTO for API exceptions.
 *
 * <p>
 * This class is used to return consistent error responses across the application.
 * It is typically used by the GlobalExceptionHandler to handle all exceptions.
 * </p>
 *
 * <p>
 * Benefits:
 * - Provides uniform error structure
 * - Improves API usability and debugging
 * - Helps frontend handle errors consistently
 * </p>
 *
 * <p>
 * Lombok Annotations:
 * - @Getter / @Setter → generates getters and setters
 * - @NoArgsConstructor → default constructor
 * - @AllArgsConstructor → all-args constructor
 * - @Builder → enables builder pattern for object creation
 * </p>
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ErrorResponse {

	/**
	 * Timestamp when the error occurred.
	 */
	private LocalDateTime timestamp;

	/**
	 * HTTP status code (e.g., 400, 401, 404, 500).
	 */
	private int status;

	/**
	 * Error type (e.g., "Bad Request", "Unauthorized").
	 */
	private String error;

	/**
	 * Detailed error message for debugging or user display.
	 */
	private String message;
}