package Jutjubic.RA56.dto;

import java.time.LocalDateTime;

public record PremiereVideoResponse(
        Long id,
        String title,
        String description,
        String thumbnailUrl,
        LocalDateTime scheduledAt,
        long durationSeconds
) {
}
