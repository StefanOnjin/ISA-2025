package Jutjubic.RA56.security.auth;

import Jutjubic.RA56.service.ActiveUserMetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class UserActivityTrackingFilter extends OncePerRequestFilter {
	private final ActiveUserMetricsService activeUserMetricsService;

	public UserActivityTrackingFilter(ActiveUserMetricsService activeUserMetricsService) {
		this.activeUserMetricsService = activeUserMetricsService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null
				&& authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken)) {
			activeUserMetricsService.markUserActive(authentication.getName());
		}

		filterChain.doFilter(request, response);
	}
}
