package Jutjubic.RA56.controller;

import Jutjubic.RA56.dto.AuthResponse;
import Jutjubic.RA56.dto.AuthTokenResponse;
import Jutjubic.RA56.dto.LoginRequest;
import Jutjubic.RA56.dto.RegistrationRequest;
import Jutjubic.RA56.service.AuthService;
import Jutjubic.RA56.util.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/auth")
public class AuthController {
	private final AuthService authService;
	private final TokenUtils tokenUtils;

	public AuthController(AuthService authService, TokenUtils tokenUtils) {
		this.authService = authService;
		this.tokenUtils = tokenUtils;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegistrationRequest request,
			HttpServletRequest httpRequest) {
		String activationBaseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/auth/activate")
				.queryParam("token", "")
				.build()
				.toUriString();

		authService.register(request, activationBaseUrl);
		return ResponseEntity.ok(new AuthResponse("Registration successful. Check your email to activate your account."));
	}

	@GetMapping("/activate")
	public ResponseEntity<AuthResponse> activate(@RequestParam("token") String token) {
		authService.activate(token);
		return ResponseEntity.ok(new AuthResponse("Account activated. You can now log in."));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest) {
		String ipAddress = httpRequest.getRemoteAddr();
		Authentication authentication = authService.login(request.getEmail(), request.getPassword(), ipAddress);

		String jwt = tokenUtils.generateToken(authentication.getName());
		int expiresIn = tokenUtils.getExpiresIn();

		return ResponseEntity.ok(new AuthTokenResponse(jwt, expiresIn));
	}
}
