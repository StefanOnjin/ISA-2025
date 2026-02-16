package Jutjubic.RA56.service;

import Jutjubic.RA56.domain.PopularityTop3;
import Jutjubic.RA56.domain.Video;
import Jutjubic.RA56.dto.PopularityTop3Response;
import Jutjubic.RA56.dto.PopularityVideoResponse;
import Jutjubic.RA56.dto.VideoDailyViewsRow;
import Jutjubic.RA56.repository.PopularityTop3Repository;
import Jutjubic.RA56.repository.VideoRepository;
import Jutjubic.RA56.repository.VideoViewRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PopularityEtlService {
	private final VideoViewRepository videoViewRepository;
	private final VideoRepository videoRepository;
	private final PopularityTop3Repository popularityTop3Repository;

	@Value("${app.base-url}")
	private String baseUrl;

	public PopularityEtlService(
			VideoViewRepository videoViewRepository,
			VideoRepository videoRepository,
			PopularityTop3Repository popularityTop3Repository
	) {
		this.videoViewRepository = videoViewRepository;
		this.videoRepository = videoRepository;
		this.popularityTop3Repository = popularityTop3Repository;
	}

	@Scheduled(cron = "${app.popularity-etl.cron:0 10 1 * * ?}")
	@Transactional
	public void runDailyEtl() {
		LocalDate today = LocalDate.now();
		LocalDateTime fromInclusive = today.minusDays(7).atStartOfDay();
		LocalDateTime toExclusive = today.atStartOfDay();
		List<VideoDailyViewsRow> rows = videoViewRepository.findDailyViewCountsInRange(fromInclusive, toExclusive);

		Map<Long, Long> scoresByVideoId = new HashMap<>();
		for (VideoDailyViewsRow row : rows) {
			if (row.getVideoId() == null || row.getViewDate() == null || row.getViewsCount() == null) {
				continue;
			}

			long daysAgo = ChronoUnit.DAYS.between(row.getViewDate(), today);
			if (daysAgo < 1 || daysAgo > 7) {
				continue;
			}

			long weight = 8 - daysAgo;
			long weightedScore = row.getViewsCount() * weight;
			scoresByVideoId.merge(row.getVideoId(), weightedScore, Long::sum);
		}

		List<VideoScore> orderedScores = scoresByVideoId.entrySet().stream()
				.map(entry -> new VideoScore(entry.getKey(), entry.getValue()))
				.sorted(Comparator.comparingLong(VideoScore::score).reversed()
						.thenComparingLong(VideoScore::videoId))
				.limit(3)
				.toList();

		Map<Long, Video> videosById = new HashMap<>();
		if (!orderedScores.isEmpty()) {
			List<Long> ids = orderedScores.stream().map(VideoScore::videoId).toList();
			videoRepository.findAllById(ids).forEach(video -> videosById.put(video.getId(), video));
		}

		PopularityTop3 result = new PopularityTop3();
		result.setCreatedAt(LocalDateTime.now());
		setRankedResult(result, orderedScores, videosById);
		popularityTop3Repository.save(result);
	}

	@Transactional(readOnly = true)
	public Optional<PopularityTop3Response> getLatestTop3() {
		return popularityTop3Repository.findTopByOrderByCreatedAtDesc()
				.map(this::toResponse);
	}

	private PopularityTop3Response toResponse(PopularityTop3 row) {
		List<PopularityVideoResponse> videos = new ArrayList<>(3);
		appendVideo(videos, row.getTop1Video(), row.getTop1Score());
		appendVideo(videos, row.getTop2Video(), row.getTop2Score());
		appendVideo(videos, row.getTop3Video(), row.getTop3Score());
		return new PopularityTop3Response(row.getCreatedAt(), videos);
	}

	private void appendVideo(List<PopularityVideoResponse> target, Video video, Long score) {
		if (video == null || score == null) {
			return;
		}
		target.add(new PopularityVideoResponse(
				video.getId(),
				video.getTitle(),
				baseUrl + "/api/videos/thumbnail/" + video.getThumbnailPath(),
				score
		));
	}

	private void setRankedResult(PopularityTop3 result, List<VideoScore> orderedScores, Map<Long, Video> videosById) {
		if (orderedScores.size() > 0) {
			VideoScore first = orderedScores.get(0);
			result.setTop1Video(videosById.get(first.videoId()));
			result.setTop1Score(first.score());
		}

		if (orderedScores.size() > 1) {
			VideoScore second = orderedScores.get(1);
			result.setTop2Video(videosById.get(second.videoId()));
			result.setTop2Score(second.score());
		}

		if (orderedScores.size() > 2) {
			VideoScore third = orderedScores.get(2);
			result.setTop3Video(videosById.get(third.videoId()));
			result.setTop3Score(third.score());
		}
	}

	private record VideoScore(Long videoId, Long score) {
	}
}
