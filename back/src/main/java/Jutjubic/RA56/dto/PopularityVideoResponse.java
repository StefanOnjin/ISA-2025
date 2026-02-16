package Jutjubic.RA56.dto;

public record PopularityVideoResponse(
		Long videoId,
		String title,
		String thumbnailUrl,
		Long score
) {
}
