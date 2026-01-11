package Jutjubic.RA56.repository;

import Jutjubic.RA56.domain.VideoLike;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoLikeRepository extends JpaRepository<VideoLike, Long> {
	long countByVideoId(Long videoId);

	boolean existsByVideoIdAndUserId(Long videoId, Long userId);

	Optional<VideoLike> findByVideoIdAndUserId(Long videoId, Long userId);
}
