package Jutjubic.RA56.dto;

public record TranscodingStatusResponse(
		Long videoId,
		String status,
		int progress,
		boolean ready,
		String message
) {
}
