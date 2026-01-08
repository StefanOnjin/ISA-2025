package Jutjubic.RA56.dto;

import java.time.LocalDateTime;

public record CommentResponse(
		Long id,
		String text,
		LocalDateTime createdAt,
		String authorUsername
) {}
