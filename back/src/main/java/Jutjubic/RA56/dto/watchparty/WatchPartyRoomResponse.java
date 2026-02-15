package Jutjubic.RA56.dto.watchparty;

import java.time.LocalDateTime;
import java.util.List;

public record WatchPartyRoomResponse(
		String roomCode,
		String ownerUsername,
		List<String> participants,
		LocalDateTime createdAt,
		boolean owner
) {
}
