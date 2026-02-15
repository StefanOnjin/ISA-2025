package Jutjubic.RA56.dto.watchparty;

import java.time.LocalDateTime;

public record WatchPartyCreateResponse(
		String roomCode,
		String ownerUsername,
		LocalDateTime createdAt
) {
}
