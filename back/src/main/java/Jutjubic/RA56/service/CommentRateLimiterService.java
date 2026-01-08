package Jutjubic.RA56.service;

import Jutjubic.RA56.exception.CommentRateLimitException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class CommentRateLimiterService {
	private final RateLimiterRegistry registry;
	private final RateLimiterConfig config;
	private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

	public CommentRateLimiterService() {
		this.config = RateLimiterConfig.custom()
				.limitForPeriod(60)
				.limitRefreshPeriod(Duration.ofHours(1))
				.timeoutDuration(Duration.ZERO)
				.build();
		this.registry = RateLimiterRegistry.of(config);
	}

	public void checkCommentAllowed(String userKey) {
		RateLimiter limiter = limiters.computeIfAbsent(userKey, key -> registry.rateLimiter(key, config));
		boolean allowed = limiter.acquirePermission();
		if (!allowed) {
			throw new CommentRateLimitException("Too many comments in the last hour");
		}
	}
}
