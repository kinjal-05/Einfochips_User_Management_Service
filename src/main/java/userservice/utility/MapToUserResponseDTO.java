package userservice.utility;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import userservice.dtos.UserResponseDTO;
import userservice.models.User;

@RequiredArgsConstructor
@Getter
@Component
public class MapToUserResponseDTO {
	/**
	 * Maps User entity to UserResponseDTO.
	 *
	 * Purpose:
	 * - Separates persistence model from API response model.
	 */
	public   UserResponseDTO mapToUserResponseDTO(User user) {
		return new UserResponseDTO(
				user.getId(),
				user.getEmail(),
				user.getRole(),
				user.getCreatedAt(),
				user.getUpdatedAt(),
				user.getCreatedById(),
				user.getUpdatedById()
		);
	}
}
