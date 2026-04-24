package userservice.Utility;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import userservice.exceptions.ResourceNotFoundException;
import userservice.models.User;
import userservice.repositories.UserRepository;

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
