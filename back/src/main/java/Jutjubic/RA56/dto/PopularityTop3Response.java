package Jutjubic.RA56.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PopularityTop3Response(
		LocalDateTime createdAt,
		List<PopularityVideoResponse> videos
) {
}
