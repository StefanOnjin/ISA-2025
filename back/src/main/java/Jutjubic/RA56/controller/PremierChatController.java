package Jutjubic.RA56.controller;

import Jutjubic.RA56.dto.ErrorResponse;
import Jutjubic.RA56.dto.chat.ChatMessageRequest;
import Jutjubic.RA56.dto.chat.ChatMessageResponse;
import Jutjubic.RA56.service.PremierChatService;
import java.security.Principal;
import java.util.Map;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

@Controller
public class PremierChatController {
	private final SimpMessagingTemplate messagingTemplate;
	private final PremierChatService premierChatService;

	public PremierChatController(SimpMessagingTemplate messagingTemplate, PremierChatService premierChatService) {
		this.messagingTemplate = messagingTemplate;
		this.premierChatService = premierChatService;
	}

	@MessageMapping("/premiers/{premierId}/chat.send")
	public void sendChatMessage(
			@DestinationVariable Long premierId,
			@Payload ChatMessageRequest request,
			Principal principal) {
		ChatMessageResponse response = premierChatService.createUserMessage(premierId, principal, request.text());
		messagingTemplate.convertAndSend(topicFor(premierId), response);
	}

	@MessageMapping("/premiers/{premierId}/chat.join")
	public void notifyJoined(@DestinationVariable Long premierId, Principal principal) {
		ChatMessageResponse response = premierChatService.createJoinMessage(premierId, principal);
		messagingTemplate.convertAndSend(topicFor(premierId), response);
	}

	@MessageExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class, AccessDeniedException.class })
	@SendToUser("/queue/errors")
	public ErrorResponse handleChatExceptions(RuntimeException ex) {
		return new ErrorResponse(ex.getMessage(), Map.of());
	}

	private String topicFor(Long premierId) {
		return "/topic/premiers/" + premierId + "/chat";
	}
}
