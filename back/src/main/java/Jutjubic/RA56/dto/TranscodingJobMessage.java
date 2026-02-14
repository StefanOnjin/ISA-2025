package Jutjubic.RA56.dto;

public record TranscodingJobMessage(
		Long jobId,
		Long videoId,
		String videoFileName,
		long durationSeconds
) {
}
