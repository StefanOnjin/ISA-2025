package Jutjubic.RA56.security.ws;

import Jutjubic.RA56.util.TokenUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
	private final TokenUtils tokenUtils;
	private final UserDetailsService userDetailsService;

	public WebSocketAuthChannelInterceptor(TokenUtils tokenUtils, UserDetailsService userDetailsService) {
		this.tokenUtils = tokenUtils;
		this.userDetailsService = userDetailsService;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
			return message;
		}

		String authHeader = accessor.getFirstNativeHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return message;
		}

		String token = authHeader.substring(7);
		String username = tokenUtils.getUsernameFromToken(token);
		if (username == null) {
			return message;
		}

		UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		if (!tokenUtils.validateToken(token, userDetails)) {
			return message;
		}

		UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
		accessor.setUser(authentication);
		return message;
	}
}
