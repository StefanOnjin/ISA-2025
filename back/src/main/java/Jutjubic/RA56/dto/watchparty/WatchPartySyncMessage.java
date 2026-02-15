package Jutjubic.RA56.dto.watchparty;

import java.time.LocalDateTime;

public record WatchPartySyncMessage(
		String type,
		String roomCode,
		Long videoId,
		String senderUsername,
		LocalDateTime sentAt
) {
}
