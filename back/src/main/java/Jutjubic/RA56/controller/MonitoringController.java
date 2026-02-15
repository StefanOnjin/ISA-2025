package Jutjubic.RA56.controller;

import Jutjubic.RA56.service.ActiveUserMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {
	private final ActiveUserMetricsService activeUserMetricsService;

	public MonitoringController(ActiveUserMetricsService activeUserMetricsService) {
		this.activeUserMetricsService = activeUserMetricsService;
	}

	@PostMapping("/heartbeat")
	public ResponseEntity<Void> heartbeat(Authentication authentication) {
		if (authentication != null) {
			activeUserMetricsService.markUserActive(authentication.getName());
		}
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(Authentication authentication) {
		if (authentication != null) {
			activeUserMetricsService.markUserInactive(authentication.getName());
		}
		return ResponseEntity.noContent().build();
	}
}
