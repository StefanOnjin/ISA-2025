package Jutjubic.RA56.dto;

import java.time.LocalDateTime;

public record PremierDetailResponse(
        Long id,
        String title,
        String description,
        String thumbnailUrl,
        String videoUrl,
        String hlsUrl,
        LocalDateTime scheduledAt,
        long durationSeconds,
        long streamOffsetSeconds
) {
}
