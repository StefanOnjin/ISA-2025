package Jutjubic.RA56.service;

import Jutjubic.RA56.exception.LoginRateLimitException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class LoginRateLimiterService {
	private final RateLimiterRegistry registry;
	private final RateLimiterConfig config;
	private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

	public LoginRateLimiterService() {
		this.config = RateLimiterConfig.custom()
				.limitForPeriod(5)
				.limitRefreshPeriod(Duration.ofMinutes(1))
				.timeoutDuration(Duration.ZERO)
				.build();
		this.registry = RateLimiterRegistry.of(config);
	}

	public void checkLoginAllowed(String ipAddress) {
		RateLimiter limiter = limiters.computeIfAbsent(ipAddress, key -> registry.rateLimiter(key, config));
		boolean allowed = limiter.acquirePermission();
		if (!allowed) {
			throw new LoginRateLimitException("Too many login attempts from this IP address");
		}
	}
}
