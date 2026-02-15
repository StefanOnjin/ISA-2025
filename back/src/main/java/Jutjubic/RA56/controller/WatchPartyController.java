package Jutjubic.RA56.controller;

import Jutjubic.RA56.dto.watchparty.WatchPartyCreateResponse;
import Jutjubic.RA56.dto.watchparty.WatchPartyRoomResponse;
import Jutjubic.RA56.service.WatchPartyService;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/watch-party")
public class WatchPartyController {
	private final WatchPartyService watchPartyService;

	public WatchPartyController(WatchPartyService watchPartyService) {
		this.watchPartyService = watchPartyService;
	}

	@PostMapping("/rooms")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<WatchPartyCreateResponse> createRoom(Principal principal) {
		return ResponseEntity.ok(watchPartyService.createRoom(principal));
	}

	@PostMapping("/rooms/{roomCode}/join")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<WatchPartyRoomResponse> joinRoom(@PathVariable String roomCode, Principal principal) {
		return ResponseEntity.ok(watchPartyService.joinRoom(roomCode, principal));
	}

	@GetMapping("/rooms/{roomCode}")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<WatchPartyRoomResponse> getRoom(@PathVariable String roomCode, Principal principal) {
		return ResponseEntity.ok(watchPartyService.getRoom(roomCode, principal));
	}
}
