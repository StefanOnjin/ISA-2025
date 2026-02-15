package Jutjubic.RA56.service;

import Jutjubic.RA56.domain.User;
import Jutjubic.RA56.domain.Video;
import Jutjubic.RA56.dto.watchparty.WatchPartyCreateResponse;
import Jutjubic.RA56.dto.watchparty.WatchPartyRoomResponse;
import Jutjubic.RA56.dto.watchparty.WatchPartySyncMessage;
import Jutjubic.RA56.repository.UserRepository;
import Jutjubic.RA56.repository.VideoRepository;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class WatchPartyService {
	private static final int ROOM_CODE_LENGTH = 6;
	private static final String ROOM_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final int MAX_ROOM_CODE_ATTEMPTS = 50;

	private final Map<String, Room> rooms = new ConcurrentHashMap<>();
	private final UserRepository userRepository;
	private final VideoRepository videoRepository;

	public WatchPartyService(UserRepository userRepository, VideoRepository videoRepository) {
		this.userRepository = userRepository;
		this.videoRepository = videoRepository;
	}

	public WatchPartyCreateResponse createRoom(Principal principal) {
		String ownerUsername = resolveAuthenticatedUsername(principal);
		String roomCode = generateUniqueRoomCode();
		LocalDateTime now = LocalDateTime.now();

		Room room = new Room(roomCode, ownerUsername, now);
		room.participants().add(ownerUsername);
		rooms.put(roomCode, room);

		return new WatchPartyCreateResponse(roomCode, ownerUsername, now);
	}

	public WatchPartyRoomResponse joinRoom(String roomCode, Principal principal) {
		String username = resolveAuthenticatedUsername(principal);
		Room room = requireRoom(roomCode);
		room.participants().add(username);
		return mapRoom(room, username);
	}

	public WatchPartyRoomResponse getRoom(String roomCode, Principal principal) {
		String username = resolveAuthenticatedUsername(principal);
		Room room = requireRoom(roomCode);
		if (!room.participants().contains(username)) {
			throw new AccessDeniedException("You must join this watch party room first.");
		}
		return mapRoom(room, username);
	}

	public WatchPartySyncMessage createPlayMessage(String roomCode, Principal principal, Long videoId) {
		if (videoId == null || videoId <= 0) {
			throw new IllegalArgumentException("Video id is required.");
		}

		Room room = requireRoom(roomCode);
		String senderUsername = resolveAuthenticatedUsername(principal);

		if (!room.participants().contains(senderUsername)) {
			throw new AccessDeniedException("You must join this watch party room first.");
		}
		if (!room.ownerUsername().equals(senderUsername)) {
			throw new AccessDeniedException("Only room owner can sync a video.");
		}

		Video video = videoRepository.findById(videoId)
				.orElseThrow(() -> new IllegalArgumentException("Video not found with id: " + videoId));

		return new WatchPartySyncMessage(
				"VIDEO_SELECTED",
				room.roomCode(),
				video.getId(),
				senderUsername,
				LocalDateTime.now());
	}

	public String normalizeRoomCode(String roomCode) {
		if (roomCode == null || roomCode.isBlank()) {
			throw new IllegalArgumentException("Room code is required.");
		}
		return roomCode.trim().toUpperCase(Locale.ROOT);
	}

	private Room requireRoom(String roomCode) {
		String normalized = normalizeRoomCode(roomCode);
		Room room = rooms.get(normalized);
		if (room == null) {
			throw new IllegalArgumentException("Watch party room not found.");
		}
		return room;
	}

	private WatchPartyRoomResponse mapRoom(Room room, String username) {
		List<String> participants = new ArrayList<>(room.participants());
		participants.sort(Comparator.naturalOrder());
		return new WatchPartyRoomResponse(
				room.roomCode(),
				room.ownerUsername(),
				participants,
				room.createdAt(),
				room.ownerUsername().equals(username));
	}

	private String resolveAuthenticatedUsername(Principal principal) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			throw new AccessDeniedException("Authentication is required.");
		}

		User user = userRepository.findByEmailIgnoreCase(principal.getName())
				.orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
		return user.getUsername();
	}

	private String generateUniqueRoomCode() {
		for (int attempt = 0; attempt < MAX_ROOM_CODE_ATTEMPTS; attempt++) {
			String candidate = randomRoomCode();
			if (!rooms.containsKey(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("Failed to generate unique room code.");
	}

	private String randomRoomCode() {
		StringBuilder builder = new StringBuilder(ROOM_CODE_LENGTH);
		for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
			int index = ThreadLocalRandom.current().nextInt(ROOM_CODE_ALPHABET.length());
			builder.append(ROOM_CODE_ALPHABET.charAt(index));
		}
		return builder.toString();
	}

	private record Room(
			String roomCode,
			String ownerUsername,
			LocalDateTime createdAt,
			Set<String> participants
	) {
		private Room(String roomCode, String ownerUsername, LocalDateTime createdAt) {
			this(roomCode, ownerUsername, createdAt, ConcurrentHashMap.newKeySet());
		}
	}
}
