package Jutjubic.RA56.controller;

import Jutjubic.RA56.dto.UserProfileResponse;
import Jutjubic.RA56.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/{username}")
	public ResponseEntity<UserProfileResponse> getProfile(@PathVariable String username) {
		return ResponseEntity.ok(userService.getPublicProfile(username));
	}
}
