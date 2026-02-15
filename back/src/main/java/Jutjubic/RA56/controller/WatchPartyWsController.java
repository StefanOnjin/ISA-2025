package Jutjubic.RA56.controller;

import Jutjubic.RA56.dto.ErrorResponse;
import Jutjubic.RA56.dto.watchparty.WatchPartyPlayRequest;
import Jutjubic.RA56.dto.watchparty.WatchPartySyncMessage;
import Jutjubic.RA56.service.WatchPartyService;
import java.security.Principal;
import java.util.Map;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

@Controller
public class WatchPartyWsController {
	private final SimpMessagingTemplate messagingTemplate;
	private final WatchPartyService watchPartyService;

	public WatchPartyWsController(SimpMessagingTemplate messagingTemplate, WatchPartyService watchPartyService) {
		this.messagingTemplate = messagingTemplate;
		this.watchPartyService = watchPartyService;
	}

	@MessageMapping("/watch-party/{roomCode}/play")
	public void playVideo(
			@DestinationVariable String roomCode,
			@Payload WatchPartyPlayRequest request,
			Principal principal) {
		WatchPartySyncMessage message =
				watchPartyService.createPlayMessage(roomCode, principal, request != null ? request.videoId() : null);
		messagingTemplate.convertAndSend(topicFor(message.roomCode()), message);
	}

	@MessageExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class, AccessDeniedException.class })
	@SendToUser("/queue/errors")
	public ErrorResponse handleWatchPartyExceptions(RuntimeException ex) {
		return new ErrorResponse(ex.getMessage(), Map.of());
	}

	private String topicFor(String roomCode) {
		return "/topic/watch-party/" + watchPartyService.normalizeRoomCode(roomCode);
	}
}
