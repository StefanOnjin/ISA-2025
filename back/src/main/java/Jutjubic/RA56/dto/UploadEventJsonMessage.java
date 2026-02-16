package Jutjubic.RA56.dto;

public record UploadEventJsonMessage(
		Long videoId,
		String title,
		long sizeBytes,
		String authorUsername,
		long createdAtEpochMs
) {
}
