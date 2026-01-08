package Jutjubic.RA56.service;

import Jutjubic.RA56.domain.Comment;
import Jutjubic.RA56.domain.User;
import Jutjubic.RA56.domain.Video;
import Jutjubic.RA56.dto.CommentResponse;
import Jutjubic.RA56.repository.CommentRepository;
import Jutjubic.RA56.repository.UserRepository;
import Jutjubic.RA56.repository.VideoRepository;
import java.time.LocalDateTime;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {
	private final CommentRepository commentRepository;
	private final VideoRepository videoRepository;
	private final UserRepository userRepository;
	private final CommentRateLimiterService rateLimiterService;

	public CommentService(CommentRepository commentRepository,
			VideoRepository videoRepository,
			UserRepository userRepository,
			CommentRateLimiterService rateLimiterService) {
		this.commentRepository = commentRepository;
		this.videoRepository = videoRepository;
		this.userRepository = userRepository;
		this.rateLimiterService = rateLimiterService;
	}

	@Cacheable(value = "comments", key = "#videoId + '-' + #page + '-' + #size")
	public Page<CommentResponse> getComments(Long videoId, int page, int size) {
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		return commentRepository.findByVideoIdOrderByCreatedAtDesc(videoId, pageRequest)
				.map(comment -> new CommentResponse(
						comment.getId(),
						comment.getText(),
						comment.getCreatedAt(),
						comment.getAuthor().getUsername()
				));
	}

	@Transactional
	@CacheEvict(value = "comments", allEntries = true)
	public CommentResponse addComment(Long videoId, String userKey, String text) {
		rateLimiterService.checkCommentAllowed(userKey);

		Video video = videoRepository.findById(videoId)
				.orElseThrow(() -> new RuntimeException("Video not found with id: " + videoId));
		User user = userRepository.findByEmailIgnoreCase(userKey)
				.orElseThrow(() -> new RuntimeException("User not found with username: " + userKey));

		Comment comment = new Comment();
		comment.setText(text);
		comment.setCreatedAt(LocalDateTime.now());
		comment.setAuthor(user);
		comment.setVideo(video);

		Comment saved = commentRepository.save(comment);
		return new CommentResponse(
				saved.getId(),
				saved.getText(),
				saved.getCreatedAt(),
				saved.getAuthor().getUsername()
		);
	}
}
