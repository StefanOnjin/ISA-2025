package Jutjubic.RA56.security.auth;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import Jutjubic.RA56.util.TokenUtils;

public class TokenAuthenticationFilter extends OncePerRequestFilter {
	private static final Log logger = LogFactory.getLog(TokenAuthenticationFilter.class);

	private final TokenUtils tokenUtils;
	private final UserDetailsService userDetailsService;

	public TokenAuthenticationFilter(TokenUtils tokenUtils, UserDetailsService userDetailsService) {
		this.tokenUtils = tokenUtils;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		String authToken = tokenUtils.getToken(request);

		try {
			if (authToken != null) {
				String username = tokenUtils.getUsernameFromToken(authToken);
				if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
					UserDetails userDetails = userDetailsService.loadUserByUsername(username);
					if (tokenUtils.validateToken(authToken, userDetails)) {
						UsernamePasswordAuthenticationToken authentication =
								new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
						SecurityContextHolder.getContext().setAuthentication(authentication);
					}
				}
			}
		} catch (ExpiredJwtException ex) {
			logger.debug("Token expired.");
		}

		chain.doFilter(request, response);
	}
}
