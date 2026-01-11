package Jutjubic.RA56.service;

import Jutjubic.RA56.domain.User;
import Jutjubic.RA56.domain.Video;
import Jutjubic.RA56.domain.VideoLike;
import Jutjubic.RA56.dto.LikeResponse;
import Jutjubic.RA56.repository.UserRepository;
import Jutjubic.RA56.repository.VideoLikeRepository;
import Jutjubic.RA56.repository.VideoRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {
	private final VideoLikeRepository likeRepository;
	private final VideoRepository videoRepository;
	private final UserRepository userRepository;

	public LikeService(VideoLikeRepository likeRepository,
			VideoRepository videoRepository,
			UserRepository userRepository) {
		this.likeRepository = likeRepository;
		this.videoRepository = videoRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public LikeResponse likeVideo(Long videoId, String userEmail) {
		Video video = videoRepository.findById(videoId)
				.orElseThrow(() -> new RuntimeException("Video not found with id: " + videoId));
		User user = userRepository.findByEmailIgnoreCase(userEmail)
				.orElseThrow(() -> new RuntimeException("User not found with username: " + userEmail));

		if (likeRepository.existsByVideoIdAndUserId(videoId, user.getId())) {
			throw new IllegalArgumentException("Video is already liked");
		}

		VideoLike like = new VideoLike();
		like.setVideo(video);
		like.setUser(user);
		like.setCreatedAt(LocalDateTime.now());
		likeRepository.save(like);

		long likesCount = likeRepository.countByVideoId(videoId);
		return new LikeResponse(videoId, likesCount, true);
	}

	@Transactional
	public LikeResponse unlikeVideo(Long videoId, String userEmail) {
		User user = userRepository.findByEmailIgnoreCase(userEmail)
				.orElseThrow(() -> new RuntimeException("User not found with username: " + userEmail));

		VideoLike like = likeRepository.findByVideoIdAndUserId(videoId, user.getId())
				.orElseThrow(() -> new IllegalArgumentException("Like does not exist for this video"));

		likeRepository.delete(like);

		long likesCount = likeRepository.countByVideoId(videoId);
		return new LikeResponse(videoId, likesCount, false);
	}
}
