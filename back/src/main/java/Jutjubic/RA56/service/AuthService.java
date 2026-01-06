package Jutjubic.RA56.service;

import Jutjubic.RA56.domain.ActivationToken;
import Jutjubic.RA56.domain.User;
import Jutjubic.RA56.dto.RegistrationRequest;
import Jutjubic.RA56.exception.ActivationTokenException;
import Jutjubic.RA56.repository.ActivationTokenRepository;
import Jutjubic.RA56.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
	private final UserRepository userRepository;
	private final ActivationTokenRepository tokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final AuthenticationManager authenticationManager;
	private final LoginRateLimiterService rateLimiterService;
	private final String activationBaseUrl;

	public AuthService(UserRepository userRepository,
			ActivationTokenRepository tokenRepository,
			PasswordEncoder passwordEncoder,
			EmailService emailService,
			AuthenticationManager authenticationManager,
			LoginRateLimiterService rateLimiterService,
			@Value("${app.activation.base-url:}") String activationBaseUrl) {
		this.userRepository = userRepository;
		this.tokenRepository = tokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.emailService = emailService;
		this.authenticationManager = authenticationManager;
		this.rateLimiterService = rateLimiterService;
		this.activationBaseUrl = activationBaseUrl;
	}

	public void register(RegistrationRequest request, String requestActivationBaseUrl) {
		if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
			throw new IllegalArgumentException("Email is already in use");
		}
		if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
			throw new IllegalArgumentException("Username is already in use");
		}

		User user = new User();
		user.setEmail(request.getEmail());
		user.setUsername(request.getUsername());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setFirstName(request.getFirstName());
		user.setLastName(request.getLastName());
		user.setAddress(request.getAddress());
		user.setEnabled(false);

		user = userRepository.save(user);

		ActivationToken token = new ActivationToken();
		token.setUser(user);
		token.setToken(UUID.randomUUID().toString());
		token.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
		tokenRepository.save(token);

		String baseUrl = activationBaseUrl != null && !activationBaseUrl.isBlank()
				? activationBaseUrl
				: requestActivationBaseUrl;
		String activationLink = baseUrl + token.getToken();

		emailService.sendActivationEmail(user.getEmail(), user.getUsername(), activationLink);
	}

	@Transactional
	public void activate(String tokenValue) {
		ActivationToken token = tokenRepository.findByToken(tokenValue)
				.orElseThrow(() -> new ActivationTokenException("Invalid activation token"));

		if (token.getExpiresAt().isBefore(Instant.now())) {
			tokenRepository.delete(token);
			throw new ActivationTokenException("Activation token has expired");
		}

		User user = token.getUser();
		user.setEnabled(true);
		userRepository.save(user);
		tokenRepository.delete(token);
	}

	public Authentication login(String email, String password, String ipAddress) {
		rateLimiterService.checkLoginAllowed(ipAddress);
		UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);
		return authenticationManager.authenticate(authToken);
	}
}
