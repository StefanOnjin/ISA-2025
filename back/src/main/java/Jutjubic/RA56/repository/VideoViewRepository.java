package Jutjubic.RA56.repository;

import Jutjubic.RA56.domain.VideoView;
import Jutjubic.RA56.dto.VideoDailyViewsRow;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoViewRepository extends JpaRepository<VideoView, Long> {
	@Query(
			value = """
					SELECT vv.video_id AS videoId,
					       DATE(vv.viewed_at) AS viewDate,
					       COUNT(*) AS viewsCount
					FROM video_views vv
					WHERE vv.viewed_at >= :fromInclusive
					  AND vv.viewed_at < :toExclusive
					GROUP BY vv.video_id, DATE(vv.viewed_at)
					""",
			nativeQuery = true
	)
	List<VideoDailyViewsRow> findDailyViewCountsInRange(
			@Param("fromInclusive") LocalDateTime fromInclusive,
			@Param("toExclusive") LocalDateTime toExclusive
	);
}
