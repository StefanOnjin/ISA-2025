package Jutjubic.RA56.dto;

public record LikeResponse(
		Long videoId,
		long likesCount,
		boolean likedByUser
) {
}
