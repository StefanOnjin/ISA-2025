package Jutjubic.RA56.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ActiveUserMetricsService {
	private final ConcurrentMap<String, Instant> liveUsers = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Instant> recentlyActiveUsers = new ConcurrentHashMap<>();
	private final long liveTimeoutMs;
	private final long recentActivityTimeoutMs;

	public ActiveUserMetricsService(
			MeterRegistry meterRegistry,
			@Value("${app.metrics.active-users.live-timeout-ms:300000}") long liveTimeoutMs,
			@Value("${app.metrics.active-users.timeout-ms:86400000}") long recentActivityTimeoutMs) {
		this.liveTimeoutMs = liveTimeoutMs;
		this.recentActivityTimeoutMs = recentActivityTimeoutMs;
		Gauge.builder("app_active_users_current", liveUsers, users -> countUsersWithinWindow(users, liveThreshold()))
				.description("Currently live users in the configured live activity window")
				.register(meterRegistry);
		Gauge.builder("app_active_users_last_24h", recentlyActiveUsers,
				users -> countUsersWithinWindow(users, recentThreshold()))
				.description("Users active at least once in the configured recent activity window")
				.register(meterRegistry);
	}

	public void markUserActive(String userKey) {
		if (userKey == null || userKey.isBlank()) {
			return;
		}
		Instant now = Instant.now();
		liveUsers.put(userKey, now);
		recentlyActiveUsers.put(userKey, now);
	}

	public void markUserInactive(String userKey) {
		if (userKey == null || userKey.isBlank()) {
			return;
		}
		liveUsers.remove(userKey);
	}

	@Scheduled(fixedDelayString = "${app.metrics.active-users.cleanup-interval-ms:60000}")
	public void cleanupExpiredUsers() {
		Instant liveThreshold = liveThreshold();
		Instant recentThreshold = recentThreshold();
		liveUsers.entrySet().removeIf(entry -> entry.getValue().isBefore(liveThreshold));
		recentlyActiveUsers.entrySet().removeIf(entry -> entry.getValue().isBefore(recentThreshold));
	}

	private double countUsersWithinWindow(Map<String, Instant> users, Instant threshold) {
		return users.values().stream()
				.filter(lastSeen -> !lastSeen.isBefore(threshold))
				.count();
	}

	private Instant liveThreshold() {
		return Instant.now().minusMillis(liveTimeoutMs);
	}

	private Instant recentThreshold() {
		return Instant.now().minusMillis(recentActivityTimeoutMs);
	}
}
