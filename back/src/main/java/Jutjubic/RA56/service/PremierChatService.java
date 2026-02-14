package Jutjubic.RA56.service;

import Jutjubic.RA56.domain.User;
import Jutjubic.RA56.domain.Video;
import Jutjubic.RA56.dto.chat.ChatMessageResponse;
import Jutjubic.RA56.repository.UserRepository;
import Jutjubic.RA56.repository.VideoRepository;
import java.security.Principal;
import java.time.LocalDateTime;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class PremierChatService {
	private static final int MAX_MESSAGE_LENGTH = 500;

	private final VideoRepository videoRepository;
	private final UserRepository userRepository;

	public PremierChatService(VideoRepository videoRepository, UserRepository userRepository) {
		this.videoRepository = videoRepository;
		this.userRepository = userRepository;
	}

	public ChatMessageResponse createUserMessage(Long premierId, Principal principal, String text) {
		validatePremierIsLive(premierId);
		String senderUsername = resolveAuthenticatedUsername(principal);
		String normalizedText = normalizeAndValidateText(text);
		return new ChatMessageResponse("CHAT", senderUsername, normalizedText, LocalDateTime.now());
	}

	public ChatMessageResponse createJoinMessage(Long premierId, Principal principal) {
		validatePremierIsLive(premierId);
		String senderUsername = resolveAuthenticatedUsername(principal);
		String text = senderUsername + " joined the stream.";
		return new ChatMessageResponse("SYSTEM", senderUsername, text, LocalDateTime.now());
	}

	private String resolveAuthenticatedUsername(Principal principal) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			throw new AccessDeniedException("Authentication is required for chat messages.");
		}

		User user = userRepository.findByEmailIgnoreCase(principal.getName())
				.orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
		return user.getUsername();
	}

	private String normalizeAndValidateText(String text) {
		if (text == null) {
			throw new IllegalArgumentException("Message cannot be empty.");
		}

		String normalized = text.trim();
		if (normalized.isBlank()) {
			throw new IllegalArgumentException("Message cannot be empty.");
		}
		if (normalized.length() > MAX_MESSAGE_LENGTH) {
			throw new IllegalArgumentException("Message cannot exceed 500 characters.");
		}

		return normalized;
	}

	private void validatePremierIsLive(Long premierId) {
		Video video = videoRepository.findById(premierId)
				.orElseThrow(() -> new IllegalArgumentException("Video not found with id: " + premierId));

		if (!Boolean.TRUE.equals(video.getPremiereEnabled())) {
			throw new IllegalStateException("Chat is available only for premieres.");
		}

		LocalDateTime createdAt = video.getCreatedAt();
		LocalDateTime scheduledAt = video.getScheduledAt();
		if (createdAt == null || scheduledAt == null || scheduledAt.isBefore(createdAt)) {
			throw new IllegalStateException("Chat is not available for this video.");
		}

		LocalDateTime now = LocalDateTime.now();
		if (now.isBefore(scheduledAt)) {
			throw new IllegalStateException("Chat will be available once premiere starts.");
		}

		long durationSeconds = (video.getDurationSeconds() == null || video.getDurationSeconds() <= 0)
				? 1L
				: video.getDurationSeconds();
		LocalDateTime endAt = scheduledAt.plusSeconds(durationSeconds);
		if (!now.isBefore(endAt)) {
			throw new IllegalStateException("Chat is closed because premiere has ended.");
		}
	}
}
