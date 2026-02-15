package Jutjubic.RA56.service;

import Jutjubic.RA56.domain.User;
import Jutjubic.RA56.domain.Video;
import Jutjubic.RA56.dto.watchparty.WatchPartyCreateResponse;
import Jutjubic.RA56.dto.watchparty.WatchPartyRoomResponse;
import Jutjubic.RA56.dto.watchparty.WatchPartySyncMessage;
import Jutjubic.RA56.repository.UserRepository;
import Jutjubic.RA56.repository.VideoRepository;
import java.security.Principal;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WatchPartyServiceTests {
	@Mock
	private UserRepository userRepository;

	@Mock
	private VideoRepository videoRepository;

	@InjectMocks
	private WatchPartyService watchPartyService;

	@Test
	void ownerCanCreateJoinAndSyncVideo() {
		User owner = new User();
		owner.setEmail("owner@example.com");
		owner.setUsername("ownerUser");

		Video video = new Video();
		video.setId(15L);

		when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
		when(videoRepository.findById(15L)).thenReturn(Optional.of(video));

		Principal principal = () -> "owner@example.com";

		WatchPartyCreateResponse created = watchPartyService.createRoom(principal);
		WatchPartyRoomResponse joined = watchPartyService.joinRoom(created.roomCode(), principal);
		WatchPartySyncMessage message = watchPartyService.createPlayMessage(created.roomCode(), principal, 15L);

		Assertions.assertEquals("ownerUser", created.ownerUsername());
		Assertions.assertTrue(joined.owner());
		Assertions.assertEquals(15L, message.videoId());
		Assertions.assertEquals("VIDEO_SELECTED", message.type());
	}

	@Test
	void nonOwnerCannotSyncVideo() {
		User owner = new User();
		owner.setEmail("owner@example.com");
		owner.setUsername("ownerUser");

		User member = new User();
		member.setEmail("member@example.com");
		member.setUsername("memberUser");

		when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
		when(userRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(member));
		when(videoRepository.findById(9L)).thenReturn(Optional.of(new Video()));
		when(userRepository.findByEmailIgnoreCase(anyString())).thenAnswer(invocation -> {
			String email = invocation.getArgument(0, String.class);
			if ("owner@example.com".equals(email)) {
				return Optional.of(owner);
			}
			if ("member@example.com".equals(email)) {
				return Optional.of(member);
			}
			return Optional.empty();
		});

		String roomCode = watchPartyService.createRoom(() -> "owner@example.com").roomCode();
		watchPartyService.joinRoom(roomCode, () -> "member@example.com");

		Assertions.assertThrows(
				org.springframework.security.access.AccessDeniedException.class,
				() -> watchPartyService.createPlayMessage(roomCode, () -> "member@example.com", 9L));
	}
}
