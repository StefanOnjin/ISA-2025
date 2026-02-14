package Jutjubic.RA56.dto.chat;

import java.time.LocalDateTime;

public record ChatMessageResponse(
		String type,
		String senderUsername,
		String text,
		LocalDateTime sentAt
) {
}
