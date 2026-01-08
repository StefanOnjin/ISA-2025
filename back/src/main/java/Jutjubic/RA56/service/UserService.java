package Jutjubic.RA56.service;

import Jutjubic.RA56.domain.User;
import Jutjubic.RA56.dto.UserProfileResponse;
import Jutjubic.RA56.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserProfileResponse getPublicProfile(String username) {
		User user = userRepository.findByUsernameIgnoreCase(username)
				.orElseThrow(() -> new RuntimeException("User not found with username: " + username));

		return new UserProfileResponse(
				user.getUsername(),
				user.getFirstName(),
				user.getLastName()
		);
	}
}
